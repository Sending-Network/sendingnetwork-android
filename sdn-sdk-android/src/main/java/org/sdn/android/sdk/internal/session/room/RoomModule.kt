/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sdn.android.sdk.internal.session.room

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.commonmark.Extension
import org.commonmark.ext.maths.MathsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.sdn.android.sdk.api.session.file.FileService
import org.sdn.android.sdk.api.session.room.RoomDirectoryService
import org.sdn.android.sdk.api.session.room.RoomService
import org.sdn.android.sdk.api.session.space.SpaceService
import org.sdn.android.sdk.internal.session.DefaultFileService
import org.sdn.android.sdk.internal.session.SessionScope
import org.sdn.android.sdk.internal.session.directory.DirectoryAPI
import org.sdn.android.sdk.internal.session.identity.DefaultSign3pidInvitationTask
import org.sdn.android.sdk.internal.session.identity.Sign3pidInvitationTask
import org.sdn.android.sdk.internal.session.room.accountdata.DefaultUpdateRoomAccountDataTask
import org.sdn.android.sdk.internal.session.room.accountdata.UpdateRoomAccountDataTask
import org.sdn.android.sdk.internal.session.room.alias.AddRoomAliasTask
import org.sdn.android.sdk.internal.session.room.alias.DefaultAddRoomAliasTask
import org.sdn.android.sdk.internal.session.room.alias.DefaultDeleteRoomAliasTask
import org.sdn.android.sdk.internal.session.room.alias.DefaultGetRoomIdByAliasTask
import org.sdn.android.sdk.internal.session.room.alias.DefaultGetRoomLocalAliasesTask
import org.sdn.android.sdk.internal.session.room.alias.DeleteRoomAliasTask
import org.sdn.android.sdk.internal.session.room.alias.GetRoomIdByAliasTask
import org.sdn.android.sdk.internal.session.room.alias.GetRoomLocalAliasesTask
import org.sdn.android.sdk.internal.session.room.create.CreateLocalRoomStateEventsTask
import org.sdn.android.sdk.internal.session.room.create.CreateLocalRoomTask
import org.sdn.android.sdk.internal.session.room.create.CreateRoomFromLocalRoomTask
import org.sdn.android.sdk.internal.session.room.create.CreateRoomTask
import org.sdn.android.sdk.internal.session.room.meeting.DefaultMeetingURLRoomTask
import org.sdn.android.sdk.internal.session.room.meeting.MeetingURLRoomTask
import org.sdn.android.sdk.internal.session.room.create.DefaultCreateLocalRoomStateEventsTask
import org.sdn.android.sdk.internal.session.room.create.DefaultCreateLocalRoomTask
import org.sdn.android.sdk.internal.session.room.create.DefaultCreateRoomFromLocalRoomTask
import org.sdn.android.sdk.internal.session.room.create.DefaultCreateRoomTask
import org.sdn.android.sdk.internal.session.room.create.DefaultDirectMsgByAddressTask
import org.sdn.android.sdk.internal.session.room.create.DirectMsgByAddressTask
import org.sdn.android.sdk.internal.session.room.delete.DefaultDeleteLocalRoomTask
import org.sdn.android.sdk.internal.session.room.delete.DefaultDeleteRoomTask
import org.sdn.android.sdk.internal.session.room.delete.DeleteLocalRoomTask
import org.sdn.android.sdk.internal.session.room.delete.DeleteRoomTask
import org.sdn.android.sdk.internal.session.room.directory.DefaultGetPublicRoomTask
import org.sdn.android.sdk.internal.session.room.directory.DefaultGetRoomDirectoryVisibilityTask
import org.sdn.android.sdk.internal.session.room.directory.DefaultSetRoomDirectoryVisibilityTask
import org.sdn.android.sdk.internal.session.room.directory.GetPublicRoomTask
import org.sdn.android.sdk.internal.session.room.directory.GetRoomDirectoryVisibilityTask
import org.sdn.android.sdk.internal.session.room.directory.SetRoomDirectoryVisibilityTask
import org.sdn.android.sdk.internal.session.room.location.CheckIfExistingActiveLiveTask
import org.sdn.android.sdk.internal.session.room.location.DefaultCheckIfExistingActiveLiveTask
import org.sdn.android.sdk.internal.session.room.location.DefaultGetActiveBeaconInfoForUserTask
import org.sdn.android.sdk.internal.session.room.location.DefaultRedactLiveLocationShareTask
import org.sdn.android.sdk.internal.session.room.location.DefaultSendLiveLocationTask
import org.sdn.android.sdk.internal.session.room.location.DefaultSendStaticLocationTask
import org.sdn.android.sdk.internal.session.room.location.DefaultStartLiveLocationShareTask
import org.sdn.android.sdk.internal.session.room.location.DefaultStopLiveLocationShareTask
import org.sdn.android.sdk.internal.session.room.location.GetActiveBeaconInfoForUserTask
import org.sdn.android.sdk.internal.session.room.location.RedactLiveLocationShareTask
import org.sdn.android.sdk.internal.session.room.location.SendLiveLocationTask
import org.sdn.android.sdk.internal.session.room.location.SendStaticLocationTask
import org.sdn.android.sdk.internal.session.room.location.StartLiveLocationShareTask
import org.sdn.android.sdk.internal.session.room.location.StopLiveLocationShareTask
import org.sdn.android.sdk.internal.session.room.membership.DefaultLoadRoomMembersTask
import org.sdn.android.sdk.internal.session.room.membership.LoadRoomMembersTask
import org.sdn.android.sdk.internal.session.room.membership.admin.DefaultMembershipAdminTask
import org.sdn.android.sdk.internal.session.room.membership.admin.MembershipAdminTask
import org.sdn.android.sdk.internal.session.room.membership.joining.DefaultInviteTask
import org.sdn.android.sdk.internal.session.room.membership.joining.DefaultJoinRoomTask
import org.sdn.android.sdk.internal.session.room.membership.joining.InviteTask
import org.sdn.android.sdk.internal.session.room.membership.joining.JoinRoomTask
import org.sdn.android.sdk.internal.session.room.membership.leaving.DefaultLeaveRoomTask
import org.sdn.android.sdk.internal.session.room.membership.leaving.LeaveRoomTask
import org.sdn.android.sdk.internal.session.room.membership.threepid.DefaultInviteThreePidTask
import org.sdn.android.sdk.internal.session.room.membership.threepid.InviteThreePidTask
import org.sdn.android.sdk.internal.session.room.peeking.DefaultPeekRoomTask
import org.sdn.android.sdk.internal.session.room.peeking.DefaultResolveRoomStateTask
import org.sdn.android.sdk.internal.session.room.peeking.PeekRoomTask
import org.sdn.android.sdk.internal.session.room.peeking.ResolveRoomStateTask
import org.sdn.android.sdk.internal.session.room.read.DefaultMarkAllRoomsReadTask
import org.sdn.android.sdk.internal.session.room.read.DefaultSetReadMarkersTask
import org.sdn.android.sdk.internal.session.room.read.MarkAllRoomsReadTask
import org.sdn.android.sdk.internal.session.room.read.SetReadMarkersTask
import org.sdn.android.sdk.internal.session.room.relation.DefaultFetchEditHistoryTask
import org.sdn.android.sdk.internal.session.room.relation.DefaultFindReactionEventForUndoTask
import org.sdn.android.sdk.internal.session.room.relation.DefaultUpdateQuickReactionTask
import org.sdn.android.sdk.internal.session.room.relation.FetchEditHistoryTask
import org.sdn.android.sdk.internal.session.room.relation.FindReactionEventForUndoTask
import org.sdn.android.sdk.internal.session.room.relation.UpdateQuickReactionTask
import org.sdn.android.sdk.internal.session.room.relation.threads.DefaultFetchThreadSummariesTask
import org.sdn.android.sdk.internal.session.room.relation.threads.DefaultFetchThreadTimelineTask
import org.sdn.android.sdk.internal.session.room.relation.threads.FetchThreadSummariesTask
import org.sdn.android.sdk.internal.session.room.relation.threads.FetchThreadTimelineTask
import org.sdn.android.sdk.internal.session.room.reporting.DefaultReportContentTask
import org.sdn.android.sdk.internal.session.room.reporting.ReportContentTask
import org.sdn.android.sdk.internal.session.room.state.DefaultSendStateTask
import org.sdn.android.sdk.internal.session.room.state.SendStateTask
import org.sdn.android.sdk.internal.session.room.tags.AddTagToRoomTask
import org.sdn.android.sdk.internal.session.room.tags.DefaultAddTagToRoomTask
import org.sdn.android.sdk.internal.session.room.tags.DefaultDeleteTagFromRoomTask
import org.sdn.android.sdk.internal.session.room.tags.DeleteTagFromRoomTask
import org.sdn.android.sdk.internal.session.room.timeline.DefaultFetchTokenAndPaginateTask
import org.sdn.android.sdk.internal.session.room.timeline.DefaultGetContextOfEventTask
import org.sdn.android.sdk.internal.session.room.timeline.DefaultGetEventTask
import org.sdn.android.sdk.internal.session.room.timeline.DefaultPaginationTask
import org.sdn.android.sdk.internal.session.room.timeline.FetchTokenAndPaginateTask
import org.sdn.android.sdk.internal.session.room.timeline.GetContextOfEventTask
import org.sdn.android.sdk.internal.session.room.timeline.GetEventTask
import org.sdn.android.sdk.internal.session.room.timeline.PaginationTask
import org.sdn.android.sdk.internal.session.room.typing.DefaultSendTypingTask
import org.sdn.android.sdk.internal.session.room.typing.SendTypingTask
import org.sdn.android.sdk.internal.session.room.uploads.DefaultGetUploadsTask
import org.sdn.android.sdk.internal.session.room.uploads.GetUploadsTask
import org.sdn.android.sdk.internal.session.room.version.DefaultRoomVersionUpgradeTask
import org.sdn.android.sdk.internal.session.room.version.RoomVersionUpgradeTask
import org.sdn.android.sdk.internal.session.space.DefaultSpaceService
import retrofit2.Retrofit
import javax.inject.Qualifier

/**
 * Used to inject the simple commonmark Parser.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class SimpleCommonmarkParser

/**
 * Used to inject the advanced commonmark Parser.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class AdvancedCommonmarkParser

@Module
internal abstract class RoomModule {

    @Module
    companion object {
        private val extensions: List<Extension> = listOf(MathsExtension.create())

        @Provides
        @JvmStatic
        @SessionScope
        fun providesRoomAPI(retrofit: Retrofit): RoomAPI {
            return retrofit.create(RoomAPI::class.java)
        }

        @Provides
        @JvmStatic
        @SessionScope
        fun providesDirectoryAPI(retrofit: Retrofit): DirectoryAPI {
            return retrofit.create(DirectoryAPI::class.java)
        }

        @Provides
        @AdvancedCommonmarkParser
        @JvmStatic
        fun providesAdvancedParser(): Parser {
            return Parser.builder().extensions(extensions).build()
        }

        @Provides
        @SimpleCommonmarkParser
        @JvmStatic
        fun providesSimpleParser(): Parser {
            // The simple parser disables all blocks but quotes.
            // Inline parsing(bold, italic, etc) is also enabled and is not easy to disable in commonmark currently.
            return Parser.builder()
                    .enabledBlockTypes(setOf(BlockQuote::class.java))
                    .build()
        }

        @Provides
        @JvmStatic
        fun providesHtmlRenderer(): HtmlRenderer {
            return HtmlRenderer
                    .builder()
                    .extensions(extensions)
                    .softbreak("<br />")
                    .build()
        }
    }

    @Binds
    abstract fun bindRoomFactory(factory: DefaultRoomFactory): RoomFactory

    @Binds
    abstract fun bindRoomGetter(getter: DefaultRoomGetter): RoomGetter

    @Binds
    abstract fun bindRoomService(service: DefaultRoomService): RoomService

    @Binds
    abstract fun bindSpaceService(service: DefaultSpaceService): SpaceService

    @Binds
    abstract fun bindRoomDirectoryService(service: DefaultRoomDirectoryService): RoomDirectoryService

    @Binds
    abstract fun bindFileService(service: DefaultFileService): FileService

    @Binds
    abstract fun bindCreateRoomTask(task: DefaultCreateRoomTask): CreateRoomTask

    @Binds
    abstract fun bindCreateLocalRoomTask(task: DefaultCreateLocalRoomTask): CreateLocalRoomTask

    @Binds
    abstract fun bindCreateLocalRoomStateEventsTask(task: DefaultCreateLocalRoomStateEventsTask): CreateLocalRoomStateEventsTask

    @Binds
    abstract fun bindCreateRoomFromLocalRoomTask(task: DefaultCreateRoomFromLocalRoomTask): CreateRoomFromLocalRoomTask

    @Binds
    abstract fun bindDirectMsgByAddressTask(task: DefaultDirectMsgByAddressTask): DirectMsgByAddressTask

    @Binds
    abstract fun bindDeleteLocalRoomTask(task: DefaultDeleteLocalRoomTask): DeleteLocalRoomTask

    @Binds
    abstract fun bindGetPublicRoomTask(task: DefaultGetPublicRoomTask): GetPublicRoomTask

    @Binds
    abstract fun bindGetRoomDirectoryVisibilityTask(task: DefaultGetRoomDirectoryVisibilityTask): GetRoomDirectoryVisibilityTask

    @Binds
    abstract fun bindSetRoomDirectoryVisibilityTask(task: DefaultSetRoomDirectoryVisibilityTask): SetRoomDirectoryVisibilityTask

    @Binds
    abstract fun bindInviteTask(task: DefaultInviteTask): InviteTask

    @Binds
    abstract fun bindInviteThreePidTask(task: DefaultInviteThreePidTask): InviteThreePidTask

    @Binds
    abstract fun bindJoinRoomTask(task: DefaultJoinRoomTask): JoinRoomTask

    @Binds
    abstract fun bindLeaveRoomTask(task: DefaultLeaveRoomTask): LeaveRoomTask

    @Binds
    abstract fun bindDeleteRoomTask(task: DefaultDeleteRoomTask): DeleteRoomTask

    @Binds
    abstract fun bindMeetingURLRoomTask(task: DefaultMeetingURLRoomTask): MeetingURLRoomTask

    @Binds
    abstract fun bindMembershipAdminTask(task: DefaultMembershipAdminTask): MembershipAdminTask

    @Binds
    abstract fun bindLoadRoomMembersTask(task: DefaultLoadRoomMembersTask): LoadRoomMembersTask

    @Binds
    abstract fun bindSetReadMarkersTask(task: DefaultSetReadMarkersTask): SetReadMarkersTask

    @Binds
    abstract fun bindMarkAllRoomsReadTask(task: DefaultMarkAllRoomsReadTask): MarkAllRoomsReadTask

    @Binds
    abstract fun bindFindReactionEventForUndoTask(task: DefaultFindReactionEventForUndoTask): FindReactionEventForUndoTask

    @Binds
    abstract fun bindUpdateQuickReactionTask(task: DefaultUpdateQuickReactionTask): UpdateQuickReactionTask

    @Binds
    abstract fun bindSendStateTask(task: DefaultSendStateTask): SendStateTask

    @Binds
    abstract fun bindReportContentTask(task: DefaultReportContentTask): ReportContentTask

    @Binds
    abstract fun bindGetContextOfEventTask(task: DefaultGetContextOfEventTask): GetContextOfEventTask

    @Binds
    abstract fun bindPaginationTask(task: DefaultPaginationTask): PaginationTask

    @Binds
    abstract fun bindFetchNextTokenAndPaginateTask(task: DefaultFetchTokenAndPaginateTask): FetchTokenAndPaginateTask

    @Binds
    abstract fun bindFetchEditHistoryTask(task: DefaultFetchEditHistoryTask): FetchEditHistoryTask

    @Binds
    abstract fun bindGetRoomIdByAliasTask(task: DefaultGetRoomIdByAliasTask): GetRoomIdByAliasTask

    @Binds
    abstract fun bindGetRoomLocalAliasesTask(task: DefaultGetRoomLocalAliasesTask): GetRoomLocalAliasesTask

    @Binds
    abstract fun bindAddRoomAliasTask(task: DefaultAddRoomAliasTask): AddRoomAliasTask

    @Binds
    abstract fun bindDeleteRoomAliasTask(task: DefaultDeleteRoomAliasTask): DeleteRoomAliasTask

    @Binds
    abstract fun bindSendTypingTask(task: DefaultSendTypingTask): SendTypingTask

    @Binds
    abstract fun bindGetUploadsTask(task: DefaultGetUploadsTask): GetUploadsTask

    @Binds
    abstract fun bindAddTagToRoomTask(task: DefaultAddTagToRoomTask): AddTagToRoomTask

    @Binds
    abstract fun bindDeleteTagFromRoomTask(task: DefaultDeleteTagFromRoomTask): DeleteTagFromRoomTask

    @Binds
    abstract fun bindResolveRoomStateTask(task: DefaultResolveRoomStateTask): ResolveRoomStateTask

    @Binds
    abstract fun bindPeekRoomTask(task: DefaultPeekRoomTask): PeekRoomTask

    @Binds
    abstract fun bindUpdateRoomAccountDataTask(task: DefaultUpdateRoomAccountDataTask): UpdateRoomAccountDataTask

    @Binds
    abstract fun bindGetEventTask(task: DefaultGetEventTask): GetEventTask

    @Binds
    abstract fun bindRoomVersionUpgradeTask(task: DefaultRoomVersionUpgradeTask): RoomVersionUpgradeTask

    @Binds
    abstract fun bindSign3pidInvitationTask(task: DefaultSign3pidInvitationTask): Sign3pidInvitationTask

    @Binds
    abstract fun bindGetRoomSummaryTask(task: DefaultGetRoomSummaryTask): GetRoomSummaryTask

    @Binds
    abstract fun bindFetchThreadTimelineTask(task: DefaultFetchThreadTimelineTask): FetchThreadTimelineTask

    @Binds
    abstract fun bindFetchThreadSummariesTask(task: DefaultFetchThreadSummariesTask): FetchThreadSummariesTask

    @Binds
    abstract fun bindStartLiveLocationShareTask(task: DefaultStartLiveLocationShareTask): StartLiveLocationShareTask

    @Binds
    abstract fun bindStopLiveLocationShareTask(task: DefaultStopLiveLocationShareTask): StopLiveLocationShareTask

    @Binds
    abstract fun bindSendStaticLocationTask(task: DefaultSendStaticLocationTask): SendStaticLocationTask

    @Binds
    abstract fun bindSendLiveLocationTask(task: DefaultSendLiveLocationTask): SendLiveLocationTask

    @Binds
    abstract fun bindGetActiveBeaconInfoForUserTask(task: DefaultGetActiveBeaconInfoForUserTask): GetActiveBeaconInfoForUserTask

    @Binds
    abstract fun bindCheckIfExistingActiveLiveTask(task: DefaultCheckIfExistingActiveLiveTask): CheckIfExistingActiveLiveTask

    @Binds
    abstract fun bindRedactLiveLocationShareTask(task: DefaultRedactLiveLocationShareTask): RedactLiveLocationShareTask
}
