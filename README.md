# SplitWise Project

A multi-user expense-sharing Android app (Kotlin, MVVM, Firebase).

## Firebase Database Structure (denormalized)

```
users/{uid}
  uid: String
  email: String (lowercase)
  displayName: String
  createdAt: Long

usersByEmail/{encodedEmail}: uid
  // encodedEmail is email.lowercase().replace(".", ",")

groups/{groupId}
  id: String
  name: String
  createdByUid: String
  createdAt: Long
  members/{uid}: true

userGroups/{uid}/{groupId}: true     ← reverse index for fast queries

// Note: membership is stored only in groups/{groupId}/members.
// The legacy groupMembers/{groupId}/{uid} path is not used.

expenses/{groupId}/{expenseId}
  id: String
  description: String
  amountCents: Long
  currency: String ("ILS")
  paidByUid: String
  createdAt: Long
  splits/{uid}: Long (cents)

groupActivities/{groupId}/{activityId}
  id: String
  groupId: String
  type: String ("group_created" | "member_added" | "expense_added")
  actorUid: String
  targetUid: String (for member_added)
  expenseId: String (for expense_added)
  description: String
  amountCents: Long
  createdAt: Long
```

## Why this structure?

- **userGroups** lets each user fetch only their groups (O(1) per user)
- **usersByEmail** provides O(1) email -> uid lookup for add-member-by-email
- **groups/{groupId}/members** stores membership directly on the group
- **Atomic multi-path updates** keep membership and userGroups in sync
- **groupActivities** provides a realtime timeline for key events
- Expenses in a separate top-level node for cleaner security rules

## Profile Summary

Profile metrics are computed live for the logged-in user from `userGroups` + `expenses`:
- `totalSpent`: sum of expenses paid by the user
- `totalReceived`: sum of amounts others owe the user
- `netBalance`: `totalReceived - totalOwed`
- `monthlyOverview`: monthly total spent (`yyyy-MM`)

## Firebase Security Rules (minimal authenticated-only)

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "groupActivities": {
      "$groupId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```

## Setup

1. Create a Firebase project at https://console.firebase.google.com
2. Enable **Authentication → Email/Password**
3. Enable **Realtime Database** (start in test mode, then paste the rules above)
4. Download `google-services.json` and place it in `app/`
5. Build and run: `./gradlew assembleDebug`

## Testing Realtime Sync

1. Install the app on two devices (or one device + emulator)
2. Register user A on device 1, register user B on device 2
3. User A creates a group → only User A sees it
4. User A adds User B by email → User B's home screen updates automatically
5. Either user adds an expense → the other sees it appear in realtime
