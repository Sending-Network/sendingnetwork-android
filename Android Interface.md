## sendingnetwork-andriod  document




## 2. Interface

#### 4.1. Set Nickname





```
/**
 * Update the display name for this user.
 * @param userId the userId to update the display name of
 * @param newDisplayName the new display name of the user
 */
suspend fun setDisplayName(userId: String, newDisplayName: String)
```

parameters:

| Name           | Type   | Description                                     | Required |
| -------------- | :----- | :---------------------------------------------- | :------- |
| userId         | String | the display name for this user                  | true     |
| newDisplayName | String | newDisplayName the new display name of the user | true     |



#### 4.2.Set Avatar 





```
    /**
     * Update the avatar for this user.
     * @param userId the userId to update the avatar of
     * @param newAvatarUri the new avatar uri of the user
     * @param fileName the fileName of selected image
     */
    suspend fun updateAvatar(userId: String, newAvatarUri: Uri, fileName: String)
```

Entry parameters:

| Name         | Type   | Description                    | Required |
| ------------ | :----- | :----------------------------- | :------- |
| userId       | String | the display name for this user | true     |
| newAvatarUri | String | the new avatar uri of the user | true     |


#### 5.  Send Image

```
    /**
     * Method to send a media asynchronously.
     * @param attachment the media to send
     * @param compressBeforeSending set to true to compress images before sending them
     * @param roomIds set of roomIds to where the media will be sent. The current roomId will be add to this set if not present.
     *                It can be useful to send media to multiple room. It's safe to include the current roomId in this set
     * @param rootThreadEventId when this param is not null, the Media will be sent in this specific thread
     * @param relatesTo add a relation content to the media event
     * @param additionalContent additional content to put in the event content
     * @return a [Cancelable]
     */
    fun sendMedia(
            attachment: ContentAttachmentData,
            compressBeforeSending: Boolean,
            roomIds: Set<String>,
            rootThreadEventId: String? = null,
            relatesTo: RelationDefaultContent? = null,
            additionalContent: Content? = null,
    ): Cancelable
```





 parameters:

| Name | Type | Description | Required |
| ---------- | :----- | :----------- | :------- |
| attachment | String | the media to send | true |
| compressBeforeSending | String | set to true to compress images before sending them | True |
| roomIds | String | set of roomIds to where the media will be sent. The current roomId will be add to this set if not present. | True |
| rootThreadEventId | String | when this param is not null, the Media will be sent in this specific thread | True |
| relatesTo | String | add a relation content to the media event | True |
| additionalContent | String | additional content to put in the event content | True |

Returns:

Cancelable



#### 6. Send Custom Message (Location)



```

  /**
     * Send a live location event to the room.
     * To get the beacon info event id, [startLiveLocationShare] must be called before sending live location updates.
     * @param beaconInfoEventId event id of the initial beacon info state event
     * @param latitude required latitude of the location
     * @param longitude required longitude of the location
     * @param uncertainty Accuracy of the location in meters
     */
    suspend fun sendLiveLocation(beaconInfoEventId: String, latitude: Double, longitude: Double, uncertainty: Double?): Cancelable

```

parameters:

| Name              | Type   | Description                                     | Required |
| ----------------- | :----- | :---------------------------------------------- | :------- |
| beaconInfoEventId | String | event id of the initial beacon info state event | true     |
| latitude          | Double | latitude of the location                        | True     |
| longitude         | Double | longitude of the location                       | True     |
| uncertainty       | Double | Accuracy of the location in meters              | False    |



#### 9.Message Notifications

```

enum class RoomNotificationState {
   
    ALL_MESSAGES_NOISY,

    ALL_MESSAGES,

    MENTIONS_ONLY,

    MUTE
}

suspend fun setRoomNotificationState(roomNotificationState: RoomNotificationState)
```

 parameters RoomNotificationState:

| Name               |      | Description                                                  |      |
| ------------------ | :--- | :----------------------------------------------------------- | :--- |
| ALL_MESSAGES_NOISY |      | All the messages will trigger a noisy notification.          |      |
| ALL_MESSAGES       |      | All the messages will trigger a notification.                |      |
| MENTIONS_ONLY      |      | Only the messages with user display name / user name will trigger notifications. |      |
| MUTE               |      | No notifications.                                            |      |



#### 10. Chat List Top Friends



```
/**
 * Add a tag to a room.
 */
@PUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/rooms/{roomId}/tags/{tag}")
suspend fun putTag(
        @Path("userId") userId: String,
        @Path("roomId") roomId: String,
        @Path("tag") tag: String,
        @Body body: TagBody
)
```

To top-favorite a room, use the tag m.favourite.



Entry parameters:

| Name   | Type    | Description | Required |
| ------ | :------ | :---------- | :------- |
| userId | String  | String      | true     |
| roomId | String  | String      | True     |
| tag    | String  | m.favourite | True     |
| body   | TagBody |             | True     |

#### 13 Unread Message Count (RoomSummary.notificationCount)
For roomMsg:



roomMsg

```json
{
  "account_data": {
    "events": [
      {
        "content": {
          "custom_config_key": "custom_config_value"
        },
        "type": "org.example.custom.config"
      }
    ]
  },
  "next_batch": "s72595_4483_1934",
  "presence": {
    "events": [
      {
        "content": {
          "avatar_url": "mxc://localhost/wefuiwegh8742w",
          "currently_active": false,
          "last_active_ago": 2478593,
          "presence": "online",
          "status_msg": "Making cupcakes"
        },
        "sender": "@example:localhost",
        "type": "m.presence"
      }
    ]
  },
  "rooms": {
    "invite": {
      "!696r7674:example.com": {
        "invite_state": {
          "events": [
            {
              "content": {
                "name": "My Room Name"
              },
              "sender": "@alice:example.com",
              "state_key": "",
              "type": "m.room.name"
            },
            {
              "content": {
                "membership": "invite"
              },
              "sender": "@alice:example.com",
              "state_key": "@bob:example.com",
              "type": "m.room.member"
            }
          ]
        }
      }
    },
    "join": {
      "!726s6s6q:example.com": {
        "account_data": {
          "events": [
            {
              "content": {
                "tags": {
                  "u.work": {
                    "order": 0.9
                  }
                }
              },
              "type": "m.tag"
            },
            {
              "content": {
                "custom_config_key": "custom_config_value"
              },
              "type": "org.example.custom.room.config"
            }
          ]
        },
        "ephemeral": {
          "events": [
            {
              "content": {
                "user_ids": [
                  "@alice:matrix.org",
                  "@bob:example.com"
                ]
              },
              "type": "m.typing"
            },
            {
              "content": {
                "$1435641916114394fHBLK:matrix.org": {
                  "m.read": {
                    "@rikj:jki.re": {
                      "ts": 1436451550453
                    }
                  },
                  "m.read.private": {
                    "@self:example.org": {
                      "ts": 1661384801651
                    }
                  }
                }
              },
              "type": "m.receipt"
            }
          ]
        },
        "state": {
          "events": [
            {
              "content": {
                "avatar_url": "mxc://example.org/SEsfnsuifSDFSSEF",
                "displayname": "Alice Margatroid",
                "membership": "join",
                "reason": "Looking for support"
              },
              "event_id": "$143273582443PhrSn:example.org",
              "origin_server_ts": 1432735824653,
              "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
              "sender": "@example:example.org",
              "state_key": "@alice:example.org",
              "type": "m.room.member",
              "unsigned": {
                "age": 1234
              }
            }
          ]
        },
        "summary": {
          "m.heroes": [
            "@alice:example.com",
            "@bob:example.com"
          ],
          "m.invited_member_count": 0,
          "m.joined_member_count": 2
        },
        "timeline": {
          "events": [
            {
              "content": {
                "avatar_url": "mxc://example.org/SEsfnsuifSDFSSEF",
                "displayname": "Alice Margatroid",
                "membership": "join",
                "reason": "Looking for support"
              },
              "event_id": "$143273582443PhrSn:example.org",
              "origin_server_ts": 1432735824653,
              "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
              "sender": "@example:example.org",
              "state_key": "@alice:example.org",
              "type": "m.room.member",
              "unsigned": {
                "age": 1234
              }
            },
            {
              "content": {
                "body": "This is an example text message",
                "format": "org.matrix.custom.html",
                "formatted_body": "<b>This is an example text message</b>",
                "msgtype": "m.text"
              },
              "event_id": "$143273582443PhrSn:example.org",
              "origin_server_ts": 1432735824653,
              "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
              "sender": "@example:example.org",
              "type": "m.room.message",
              "unsigned": {
                "age": 1234
              }
            }
          ],
          "limited": true,
          "prev_batch": "t34-23535_0_0"
        },
        "unread_notifications": {
          "highlight_count": 1,
          "notification_count": 5
        },
        "unread_thread_notifications": {
          "$threadroot": {
            "highlight_count": 3,
            "notification_count": 6
          }
        }
      }
    },
    "knock": {
      "!223asd456:example.com": {
        "knock_state": {
          "events": [
            {
              "content": {
                "name": "My Room Name"
              },
              "sender": "@alice:example.com",
              "state_key": "",
              "type": "m.room.name"
            },
            {
              "content": {
                "membership": "knock"
              },
              "sender": "@bob:example.com",
              "state_key": "@bob:example.com",
              "type": "m.room.member"
            }
          ]
        }
      }
    },
    "leave": {}
  }
}
```



| Name                 | Type      | Description                                                  |
| -------------------- | --------- | ------------------------------------------------------------ |
| `highlight_count`    | `integer` | The number of unread notifications for this room with the highlight flag set. |
| `notification_count` | `integer` | The total number of unread notifications for this room.      |

