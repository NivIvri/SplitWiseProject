package com.example.splitwise_project.data.repository

import com.example.splitwise_project.data.model.Group
import com.example.splitwise_project.data.model.ActivityItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Repository for groups using denormalized Firebase structure:
 *   groups/{groupId}           – group metadata
 *   groups/{groupId}/members/{uid} – membership flags
 *   userGroups/{uid}/{groupId}   – reverse index for quick listing
 */
class GroupRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val groupsRef = db.child("groups")
    private val userGroupsRef = db.child("userGroups")
    private val activitiesRef = db.child("groupActivities")

    // ── Observe ────────────────────────────────────────────────────────

    /**
     * Observe groups for a user via the userGroups/{uid} index.
     * On each change, fetches full group data for every groupId.
     * Returns a cleanup function that removes the listener.
     */
    fun observeUserGroups(
        uid: String,
        onResult: (List<Group>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupIds = snapshot.children.mapNotNull { it.key }
                if (groupIds.isEmpty()) {
                    onResult(emptyList())
                    return
                }
                fetchGroupsByIds(groupIds, onResult)
            }

            override fun onCancelled(error: DatabaseError) = onError(error)
        }
        userGroupsRef.child(uid).addValueEventListener(listener)
        return { userGroupsRef.child(uid).removeEventListener(listener) }
    }

    /** Observe a single group by id. Returns cleanup function. */
    fun observeGroup(
        groupId: String,
        onResult: (Group?) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = onResult(parseGroup(snapshot))
            override fun onCancelled(error: DatabaseError) = onError(error)
        }
        groupsRef.child(groupId).addValueEventListener(listener)
        return { groupsRef.child(groupId).removeEventListener(listener) }
    }

    /** Observe member UIDs for a group from groups/{groupId}/members. */
    fun observeGroupMembers(
        groupId: String,
        onResult: (List<String>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        val membersRef = groupsRef.child(groupId).child("members")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onResult(snapshot.children.mapNotNull { it.key })
            }
            override fun onCancelled(error: DatabaseError) = onError(error)
        }
        membersRef.addValueEventListener(listener)
        return { membersRef.removeEventListener(listener) }
    }

    /** Observe timeline activities for a group. Returns cleanup function. */
    fun observeGroupActivities(
        groupId: String,
        onResult: (List<ActivityItem>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        val ref = activitiesRef.child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { parseActivity(it, groupId) }
                onResult(list.sortedByDescending { it.createdAt })
            }

            override fun onCancelled(error: DatabaseError) = onError(error)
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    /** Observe timeline across all groups for one user. Returns cleanup function. */
    fun observeUserActivities(
        userUid: String,
        onResult: (List<ActivityItem>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        val groupListeners = mutableMapOf<String, ValueEventListener>()
        val activitiesByGroup = mutableMapOf<String, List<ActivityItem>>()
        val userGroupsNode = userGroupsRef.child(userUid)

        fun emitMerged() {
            onResult(
                activitiesByGroup.values
                    .flatten()
                    .sortedByDescending { it.createdAt }
            )
        }

        val userGroupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activeGroupIds = snapshot.children.mapNotNull { it.key }.toSet()

                // Remove listeners for groups user no longer belongs to.
                val removed = groupListeners.keys - activeGroupIds
                removed.forEach { groupId ->
                    val listener = groupListeners.remove(groupId)
                    if (listener != null) {
                        activitiesRef.child(groupId).removeEventListener(listener)
                    }
                    activitiesByGroup.remove(groupId)
                }

                // Add listeners for newly joined groups.
                val added = activeGroupIds - groupListeners.keys
                added.forEach { groupId ->
                    val listener = object : ValueEventListener {
                        override fun onDataChange(groupSnapshot: DataSnapshot) {
                            val list = groupSnapshot.children.mapNotNull { parseActivity(it, groupId) }
                            activitiesByGroup[groupId] = list
                            emitMerged()
                        }

                        override fun onCancelled(error: DatabaseError) = onError(error)
                    }
                    groupListeners[groupId] = listener
                    activitiesRef.child(groupId).addValueEventListener(listener)
                }

                emitMerged()
            }

            override fun onCancelled(error: DatabaseError) = onError(error)
        }

        userGroupsNode.addValueEventListener(userGroupsListener)

        return {
            userGroupsNode.removeEventListener(userGroupsListener)
            groupListeners.forEach { (groupId, listener) ->
                activitiesRef.child(groupId).removeEventListener(listener)
            }
            groupListeners.clear()
            activitiesByGroup.clear()
        }
    }

    // ── Write ──────────────────────────────────────────────────────────

    /**
     * Create a group and add creator as member atomically via multi-path update.
     * Writes to: groups/{id}, groups/{id}/members/{uid}, userGroups/{uid}/{id}.
     */
    fun createGroup(name: String, creatorUid: String, onDone: (Boolean) -> Unit) {
        val key = groupsRef.push().key ?: run { onDone(false); return }
        val now = System.currentTimeMillis()
        val activityId = activitiesRef.child(key).push().key ?: run { onDone(false); return }

        val updates = mapOf<String, Any>(
            "groups/$key/id" to key,
            "groups/$key/name" to name,
            "groups/$key/createdByUid" to creatorUid,
            "groups/$key/createdAt" to now,
            "groups/$key/members/$creatorUid" to true,
            "userGroups/$creatorUid/$key" to true,
            "groupActivities/$key/$activityId/id" to activityId,
            "groupActivities/$key/$activityId/groupId" to key,
            "groupActivities/$key/$activityId/type" to "group_created",
            "groupActivities/$key/$activityId/actorUid" to creatorUid,
            "groupActivities/$key/$activityId/description" to "Group created",
            "groupActivities/$key/$activityId/createdAt" to now
        )

        db.updateChildren(updates)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    /**
     * Add a member to a group atomically.
     * Writes to: groups/{groupId}/members/{uid}, userGroups/{uid}/{groupId}.
     */
    fun addMember(groupId: String, memberUid: String, actorUid: String?, onDone: (Boolean) -> Unit) {
        val activityId = activitiesRef.child(groupId).push().key ?: run { onDone(false); return }
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "groups/$groupId/members/$memberUid" to true,
            "userGroups/$memberUid/$groupId" to true,
            "groupActivities/$groupId/$activityId/id" to activityId,
            "groupActivities/$groupId/$activityId/groupId" to groupId,
            "groupActivities/$groupId/$activityId/type" to "member_added",
            "groupActivities/$groupId/$activityId/actorUid" to (actorUid ?: ""),
            "groupActivities/$groupId/$activityId/targetUid" to memberUid,
            "groupActivities/$groupId/$activityId/description" to "Member added",
            "groupActivities/$groupId/$activityId/createdAt" to now
        )
        db.updateChildren(updates)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    /** Add multiple members atomically and keep userGroups index in sync. */
    fun addMembers(
        groupId: String,
        memberUids: List<String>,
        actorUid: String?,
        onDone: (Boolean) -> Unit
    ) {
        if (memberUids.isEmpty()) {
            onDone(true)
            return
        }
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any>()
        memberUids.forEach { memberUid ->
            updates["groups/$groupId/members/$memberUid"] = true
            updates["userGroups/$memberUid/$groupId"] = true

            val activityId = activitiesRef.child(groupId).push().key
            if (!activityId.isNullOrBlank()) {
                updates["groupActivities/$groupId/$activityId/id"] = activityId
                updates["groupActivities/$groupId/$activityId/groupId"] = groupId
                updates["groupActivities/$groupId/$activityId/type"] = "member_added"
                updates["groupActivities/$groupId/$activityId/actorUid"] = actorUid ?: ""
                updates["groupActivities/$groupId/$activityId/targetUid"] = memberUid
                updates["groupActivities/$groupId/$activityId/description"] = "Member added"
                updates["groupActivities/$groupId/$activityId/createdAt"] = now
            }
        }
        db.updateChildren(updates)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    /** Append a timeline item for the given group. */
    fun addGroupActivity(item: ActivityItem, onDone: (Boolean) -> Unit) {
        val ref = activitiesRef.child(item.groupId).push()
        val key = ref.key ?: run { onDone(false); return }
        val toSave = item.copy(id = key)
        ref.setValue(activityToMap(toSave))
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    /** Fetch group data for a list of ids; calls onResult once all are fetched. */
    private fun fetchGroupsByIds(ids: List<String>, onResult: (List<Group>) -> Unit) {
        val groups = mutableListOf<Group>()
        var remaining = ids.size

        ids.forEach { id ->
            groupsRef.child(id).get()
                .addOnSuccessListener { snap ->
                    parseGroup(snap)?.let { groups.add(it) }
                    remaining--
                    if (remaining == 0) onResult(groups)
                }
                .addOnFailureListener {
                    remaining--
                    if (remaining == 0) onResult(groups)
                }
        }
    }

    private fun parseGroup(snapshot: DataSnapshot): Group? {
        val id = snapshot.key ?: return null
        val map = snapshot.value as? Map<*, *> ?: return null
        val membersMap = map["members"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        val memberUids = membersMap.keys.mapNotNull { it?.toString() }
        return Group(
            id = id,
            name = (map["name"] as? String).orEmpty(),
            createdByUid = (map["createdByUid"] as? String).orEmpty(),
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
            memberCount = memberUids.size,
            memberUids = memberUids
        )
    }

    private fun parseActivity(snapshot: DataSnapshot, groupId: String): ActivityItem? {
        val id = snapshot.key ?: return null
        val map = snapshot.value as? Map<*, *> ?: return null
        return ActivityItem(
            id = id,
            groupId = groupId,
            type = (map["type"] as? String).orEmpty(),
            actorUid = (map["actorUid"] as? String).orEmpty(),
            targetUid = (map["targetUid"] as? String).orEmpty(),
            expenseId = (map["expenseId"] as? String).orEmpty(),
            description = (map["description"] as? String).orEmpty(),
            amountCents = (map["amountCents"] as? Number)?.toLong() ?: 0L,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun activityToMap(a: ActivityItem): Map<String, Any> = mapOf(
        "id" to a.id,
        "groupId" to a.groupId,
        "type" to a.type,
        "actorUid" to a.actorUid,
        "targetUid" to a.targetUid,
        "expenseId" to a.expenseId,
        "description" to a.description,
        "amountCents" to a.amountCents,
        "createdAt" to a.createdAt
    )
}
