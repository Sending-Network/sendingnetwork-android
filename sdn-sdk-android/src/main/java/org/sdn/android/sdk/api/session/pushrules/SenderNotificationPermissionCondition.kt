/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sdn.android.sdk.api.session.pushrules

import org.sdn.android.sdk.api.session.events.model.Event
import org.sdn.android.sdk.api.session.room.model.PowerLevelsContent
import org.sdn.android.sdk.api.session.room.powerlevels.PowerLevelsHelper

class SenderNotificationPermissionCondition(
        /**
         * A string that determines the power level the sender must have to trigger notifications of a given type,
         * such as room. Refer to the m.room.power_levels event schema for information about what the defaults are
         * and how to interpret the event. The key is used to look up the power level required to send a notification
         * type from the notifications object in the power level event content.
         */
        val key: String
) : Condition {

    override fun isSatisfied(event: Event, conditionResolver: ConditionResolver): Boolean {
        return conditionResolver.resolveSenderNotificationPermissionCondition(event, this)
    }

    override fun technicalDescription() = "User power level <$key>"

    fun isSatisfied(event: Event, powerLevels: PowerLevelsContent): Boolean {
        val powerLevelsHelper = PowerLevelsHelper(powerLevels)
        return event.senderId != null && powerLevelsHelper.getUserPowerLevelValue(event.senderId) >= powerLevels.notificationLevel(key)
    }
}
