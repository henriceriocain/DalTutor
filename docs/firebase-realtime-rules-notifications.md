Realtime Database rules for in-app notifications (no Cloud Functions)

Copy this into your RTDB rules and adjust as needed. It allows:
- Only the recipient to read their notifications
- Any authed actor to create a notification for the recipient with a validated shape
- Only the recipient to mark a notification read=true; no other fields may change

{
  "rules": {
    "community_notifications": {
      "$recipient": {
        ".read": "auth != null && auth.uid == $recipient",
        "$id": {
          ".write": "auth != null && (
            (!data.exists() && newData.exists() &&
              newData.child('recipientUserId').val() == $recipient &&
              newData.child('actorUserId').val() == auth.uid &&
              (newData.child('type').val() == 'REPLY' || newData.child('type').val() == 'STAR') &&
              newData.child('threadId').isString() &&
              newData.child('threadTitle').isString() &&
              newData.child('timestamp').isNumber() &&
              (newData.child('type').val() == 'REPLY' ? newData.child('replyId').isString() : true) &&
              newData.child('read').val() == false)
            ||
            (data.exists() && auth.uid == $recipient &&
              data.child('recipientUserId').val() == newData.child('recipientUserId').val() &&
              data.child('actorUserId').val() == newData.child('actorUserId').val() &&
              data.child('type').val() == newData.child('type').val() &&
              data.child('threadId').val() == newData.child('threadId').val() &&
              data.child('threadTitle').val() == newData.child('threadTitle').val() &&
              data.child('replyId').val() == newData.child('replyId').val() &&
              data.child('timestamp').val() == newData.child('timestamp').val() &&
              data.child('read').val() == false &&
              newData.child('read').val() == true)
          )"
        }
      }
    }
  }
}

Indexes (Database > Rules > Add index):
- community_notifications/$recipient: 
  { ".indexOn": ["timestamp"] }

