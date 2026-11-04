package bo.saludencasa.features.conversation.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bo.saludencasa.R
import bo.saludencasa.core.util.AppointmentZone
import bo.saludencasa.core.util.formatFullDate
import bo.saludencasa.core.util.formatTime
import bo.saludencasa.features.conversation.domain.model.ConversationHeader
import bo.saludencasa.features.conversation.domain.model.Message
import bo.saludencasa.features.conversation.domain.model.MessageError
import bo.saludencasa.features.conversation.domain.vo.MessageContent
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.ui.components.BottomActionBar
import bo.saludencasa.ui.components.CardAvatar
import bo.saludencasa.ui.components.PrimaryButton
import bo.saludencasa.ui.components.ScreenHeader
import bo.saludencasa.ui.theme.ExtraShapes
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

internal data class ConversationActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onDraftChange: (String) -> Unit = {},
    val onRecordingChange: (Boolean) -> Unit = {},
    val onSend: () -> Unit = {},
    val onRetrySend: (String) -> Unit = {},
    val onLoadOlder: () -> Unit = {},
)

@Composable
fun ConversationScreen(
    requestId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel = koinViewModel { parametersOf(requestId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // docs/decisions.md, 2026-11-03, HU-21: open only while in the foreground.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        onPauseOrDispose { viewModel.onPaused() }
    }

    ConversationContent(
        uiState = uiState,
        today = LocalDate.now(AppointmentZone),
        actions =
            ConversationActions(
                onBack = onBack,
                onRetry = viewModel::retry,
                onDraftChange = viewModel::onDraftChange,
                onRecordingChange = viewModel::onRecordingChange,
                onSend = viewModel::send,
                onRetrySend = viewModel::retrySend,
                onLoadOlder = viewModel::loadOlder,
            ),
        modifier = modifier,
    )
}

@Composable
internal fun ConversationContent(
    uiState: ConversationUiState,
    today: LocalDate,
    actions: ConversationActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            ConversationUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    PlainHeader(onBack = actions.onBack)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            ConversationUiState.NotFound -> {
                MessageColumn(onBack = actions.onBack) {
                    Text(
                        text = stringResource(R.string.attention_not_found_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.attention_not_found_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is ConversationUiState.Failed -> {
                MessageColumn(onBack = actions.onBack) {
                    Text(
                        text = stringResource(R.string.conversation_load_failed),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(uiState.error.messageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryButton(text = stringResource(R.string.common_retry), onClick = actions.onRetry)
                }
            }

            is ConversationUiState.Content -> {
                ConversationBody(content = uiState, today = today, actions = actions)
            }
        }
    }
}

@Composable
private fun ConversationBody(
    content: ConversationUiState.Content,
    today: LocalDate,
    actions: ConversationActions,
) {
    val locale = LocalConfiguration.current.locales[0]
    val rows = remember(content.items, content.older, today) { conversationRows(content.items, content.older, today) }
    val listState = rememberLazyListState()

    // The list is reversed, so the top of the conversation is its last row.
    val nearTop by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= info.totalItemsCount - OLDER_PAGE_THRESHOLD
        }
    }
    LaunchedEffect(nearTop, rows.size) { if (nearTop) actions.onLoadOlder() }

    // A list keeps the row it shows in place, so a new message lands below the
    // screen unless the list follows it: when it is mine, or the reader was at
    // the newest already.
    val newest = rows.firstOrNull()
    LaunchedEffect(newest?.key) {
        val mine = (newest as? ConversationRow.Bubble)?.item?.sentByMe == true
        if (mine || listState.firstVisibleItemIndex <= 1) listState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ConversationHeaderRow(header = content.header, onBack = actions.onBack)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (rows.isEmpty()) {
                EmptyConversation(counterpartName = content.header.counterpartName)
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.screenMargin, vertical = Spacing.scale16),
                    verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap),
                ) {
                    items(rows, key = ConversationRow::key) { row ->
                        when (row) {
                            is ConversationRow.Bubble -> {
                                BubbleRow(
                                    item = row.item,
                                    showsReceipt = row.showsReceipt,
                                    locale = locale,
                                    onRetrySend = actions.onRetrySend,
                                )
                            }

                            is ConversationRow.Day -> {
                                DayLabel(date = row.date, today = today, locale = locale)
                            }

                            is ConversationRow.Older -> {
                                OlderRow(state = row.state, onLoadOlder = actions.onLoadOlder)
                            }

                            ConversationRow.Start -> {
                                StartNotice(counterpartName = content.header.counterpartName)
                            }
                        }
                    }
                }
            }
        }

        if (content.header.canWrite) {
            Composer(content = content, actions = actions)
        } else {
            ClosedNotice()
        }
    }
}

@Composable
private fun ConversationHeaderRow(
    header: ConversationHeader,
    onBack: () -> Unit,
) {
    val large = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(end = Spacing.screenMargin, top = Spacing.scale8, bottom = Spacing.scale8),
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack = onBack)
                // At a large font the name needs the width the avatar would take.
                if (!large) CardAvatar(name = header.counterpartName, photoUrl = header.counterpartPhotoUrl)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
                ) {
                    Text(
                        text = header.counterpartName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.conversation_header_subtitle,
                                header.serviceTypeName,
                                stringResource(header.status.labelRes()),
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (header.status == ServiceStatus.CANCELLED) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack, modifier = Modifier.size(Spacing.minTouchTarget)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back_button),
        )
    }
}

@Composable
private fun PlainHeader(onBack: () -> Unit) {
    ScreenHeader(
        title = stringResource(R.string.conversation_title),
        onBack = onBack,
        modifier = Modifier.padding(horizontal = Spacing.screenMargin, vertical = Spacing.scale8),
    )
}

@Composable
private fun BubbleRow(
    item: ConversationItem,
    showsReceipt: Boolean,
    locale: Locale,
    onRetrySend: (String) -> Unit,
) {
    val mine = item.sentByMe
    if (item.isWorkRecord) {
        WorkRecordRow(item = item, showsReceipt = showsReceipt, locale = locale, onRetrySend = onRetrySend)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(Spacing.bubbleMaxWidthFraction),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
        ) {
            Surface(
                shape = if (mine) ExtraShapes.myBubble else ExtraShapes.theirBubble,
                color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                contentColor =
                    if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                border =
                    if (mine) {
                        null
                    } else {
                        BorderStroke(
                            Spacing.borderWidth,
                            MaterialTheme.colorScheme.outlineVariant,
                        )
                    },
            ) {
                Text(
                    text = item.text(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
                )
            }
            BubbleFooter(item = item, showsReceipt = showsReceipt, locale = locale, onRetrySend = onRetrySend)
        }
    }
}

// docs/design-system.md, «Registro de la atención».
@Composable
private fun WorkRecordRow(
    item: ConversationItem,
    showsReceipt: Boolean,
    locale: Locale,
    onRetrySend: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (item.sentByMe) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(Spacing.scale4),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.cardPadding, vertical = Spacing.scale12),
                verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_work_record),
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.scale20),
                    )
                    Text(
                        text = stringResource(R.string.conversation_work_record_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(text = item.text(), style = MaterialTheme.typography.bodyLarge)
            }
        }
        BubbleFooter(item = item, showsReceipt = showsReceipt, locale = locale, onRetrySend = onRetrySend)
    }
}

private fun ConversationItem.text(): String =
    when (this) {
        is ConversationItem.Delivered -> message.content
        is ConversationItem.Sending -> text
        is ConversationItem.NotSent -> text
    }

@Composable
private fun BubbleFooter(
    item: ConversationItem,
    showsReceipt: Boolean,
    locale: Locale,
    onRetrySend: (String) -> Unit,
) {
    when (item) {
        is ConversationItem.Delivered -> {
            val time =
                formatTime(
                    item.message.createdAt
                        .atZone(AppointmentZone)
                        .toLocalTime(),
                    locale,
                )
            val read = item.message.readAt != null
            val label =
                when {
                    !showsReceipt -> time
                    read -> stringResource(R.string.conversation_status_read, time)
                    else -> stringResource(R.string.conversation_status_sent, time)
                }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FooterText(label)
                if (showsReceipt && read) {
                    Icon(
                        painter = painterResource(R.drawable.ic_done_all),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.scale16),
                    )
                }
            }
        }

        is ConversationItem.Sending -> {
            FooterText(stringResource(R.string.conversation_status_sending))
        }

        is ConversationItem.NotSent -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        stringResource(
                            if (item.error == MessageError.WorkRecordNotAllowed) {
                                R.string.error_work_record_not_allowed
                            } else {
                                R.string.conversation_not_sent
                            },
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TextButton(
                    onClick = { onRetrySend(item.key) },
                    modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
                ) {
                    Text(text = stringResource(R.string.common_retry), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun FooterText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DayLabel(
    date: LocalDate,
    today: LocalDate,
    locale: Locale,
) {
    val label =
        when (date) {
            today -> stringResource(R.string.conversation_day_today)
            today.minusDays(1) -> stringResource(R.string.conversation_day_yesterday)
            else -> formatFullDate(date, locale)
        }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OlderRow(
    state: OlderMessages,
    onLoadOlder: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.scale8, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (state) {
            OlderMessages.FAILED -> {
                Text(
                    text = stringResource(R.string.conversation_older_failed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TextButton(onClick = onLoadOlder, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                    Text(text = stringResource(R.string.common_retry), style = MaterialTheme.typography.labelLarge)
                }
            }

            OlderMessages.IDLE, OlderMessages.LOADING, OlderMessages.EXHAUSTED -> {
                val description = stringResource(R.string.conversation_older_loading)
                CircularProgressIndicator(
                    modifier = Modifier.size(Spacing.scale24).semantics { contentDescription = description },
                )
            }
        }
    }
}

@Composable
private fun StartNotice(counterpartName: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
            Text(
                text = stringResource(R.string.conversation_start_notice, counterpartName),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.scale12, vertical = Spacing.scale8),
            )
        }
    }
}

@Composable
private fun EmptyConversation(counterpartName: String) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale12, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chat),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.scale48),
        )
        Text(
            text = stringResource(R.string.conversation_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.conversation_empty_body, counterpartName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Composer(
    content: ConversationUiState.Content,
    actions: ConversationActions,
) {
    val recording = content.recording
    val inputDescription =
        stringResource(
            if (recording) R.string.cd_work_record_input else R.string.cd_message_input,
            content.header.counterpartName,
        )
    val minLines =
        when {
            !recording -> 1
            LocalDensity.current.fontScale >= LARGE_FONT_SCALE -> RECORD_MIN_LINES_LARGE_FONT
            else -> RECORD_MIN_LINES
        }
    val maxLength =
        NumberFormat
            .getIntegerInstance(
                LocalConfiguration.current.locales[0],
            ).format(MessageContent.MAX_LENGTH)
    // MainActivity pads the system bars and not the keyboard, and edge to edge the
    // window no longer resizes for it: the bar rises by the keyboard less the
    // navigation bar already padded.
    BottomActionBar(modifier = Modifier.windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))) {
        // docs/decisions.md, 2026-11-03, HU-22: saving stays within reach.
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.scale8),
        ) {
            if (content.header.canRecord) {
                WorkRecordToggle(selected = recording, onSelectedChange = actions.onRecordingChange)
            }
            if (recording) {
                WorkRecordNotices(showsStudentNotice = content.header.showsStudentNotice)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.scale8),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = content.draft,
                    onValueChange = actions.onDraftChange,
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics { contentDescription = inputDescription },
                    placeholder = {
                        Text(
                            text =
                                stringResource(
                                    if (recording) {
                                        R.string.conversation_work_record_placeholder
                                    } else {
                                        R.string.conversation_input_placeholder
                                    },
                                ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = MaterialTheme.shapes.small,
                    minLines = minLines,
                    maxLines = COMPOSER_MAX_LINES,
                    isError = content.draftError != null,
                    supportingText =
                        content.draftError?.let { error ->
                            {
                                Text(
                                    text = stringResource(error.messageRes(), maxLength),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        },
                )
                FilledIconButton(
                    onClick = actions.onSend,
                    enabled = content.canSend,
                    modifier = Modifier.size(Spacing.minTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription =
                            stringResource(if (recording) R.string.cd_save_work_record else R.string.cd_send_message),
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkRecordToggle(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = { onSelectedChange(!selected) },
        label = {
            Text(
                text = stringResource(R.string.conversation_work_record_label),
                style = MaterialTheme.typography.labelLarge,
            )
        },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_work_record),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        },
    )
}

@Composable
private fun WorkRecordNotices(showsStudentNotice: Boolean) {
    if (showsStudentNotice) {
        Text(
            text = stringResource(R.string.conversation_work_record_student_notice),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    Text(
        text = stringResource(R.string.conversation_work_record_hint),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ClosedNotice() {
    BottomActionBar {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.scale12),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.scale20),
            )
            Text(
                text = stringResource(R.string.conversation_closed_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageColumn(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenMargin)
                .padding(vertical = Spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.scale16),
    ) {
        ScreenHeader(title = stringResource(R.string.conversation_title), onBack = onBack)
        content()
    }
}

@StringRes
private fun ServiceStatus.labelRes(): Int =
    when (this) {
        ServiceStatus.ASSIGNED -> R.string.attention_status_assigned
        ServiceStatus.IN_PROGRESS -> R.string.attention_status_in_progress
        ServiceStatus.COMPLETED -> R.string.attention_status_completed
        ServiceStatus.CANCELLED -> R.string.attention_status_cancelled
    }

@StringRes
private fun MessageError.messageRes(): Int =
    when (this) {
        MessageError.Empty -> R.string.error_message_empty
        MessageError.TooLong -> R.string.error_message_too_long
        MessageError.ConversationClosed -> R.string.conversation_closed_notice
        MessageError.WorkRecordNotAllowed -> R.string.error_work_record_not_allowed
        MessageError.NetworkUnavailable -> R.string.error_network_unavailable
        MessageError.NotSignedIn, MessageError.Unexpected -> R.string.error_unexpected
    }

private const val LARGE_FONT_SCALE = 1.5f
private const val OLDER_PAGE_THRESHOLD = 3
private const val COMPOSER_MAX_LINES = 5
private const val RECORD_MIN_LINES = 3
private const val RECORD_MIN_LINES_LARGE_FONT = 2

private val previewToday: LocalDate = LocalDate.parse("2026-11-03")

private val previewHeader =
    ConversationHeader(
        counterpartName = "Ana Quispe",
        counterpartPhotoUrl = null,
        serviceTypeName = "Curación de heridas",
        status = ServiceStatus.ASSIGNED,
        canWrite = true,
    )

private fun previewMessage(
    id: String,
    content: String,
    sentByMe: Boolean,
    at: String,
    read: Boolean = true,
) = Message(
    id = id,
    content = content,
    sentByMe = sentByMe,
    createdAt = Instant.parse(at),
    readAt = if (read) Instant.parse(at) else null,
)

private val previewMessages =
    listOf(
        previewMessage(
            "m1",
            "Buen día. Estoy saliendo hacia su domicilio, llego en unos 25 minutos.",
            false,
            "2026-11-03T13:12:00Z",
        ),
        previewMessage(
            "m2",
            "Perfecto. Es el edificio azul, tercer piso, departamento 3B. El timbre no funciona: toque la puerta.",
            true,
            "2026-11-03T13:14:00Z",
        ),
        previewMessage("m3", "¿Tiene gasas en casa o llevo las mías?", false, "2026-11-03T13:15:00Z"),
        previewMessage("m4", "Tengo gasas y alcohol, pero no tengo suero.", true, "2026-11-03T13:16:00Z"),
    )

private val previewContent =
    ConversationUiState.Content(
        header = previewHeader,
        messages = previewMessages,
        older = OlderMessages.EXHAUSTED,
    )

private val previewWorkRecord =
    Message(
        id = "m5",
        content = "Curación de la herida de la pierna derecha. Limpieza con suero y cambio de apósito.",
        sentByMe = true,
        createdAt = Instant.parse("2026-11-03T15:40:00Z"),
        readAt = Instant.parse("2026-11-03T15:41:00Z"),
        isWorkRecord = true,
    )

private val previewRecording =
    previewContent.copy(
        header = previewHeader.copy(status = ServiceStatus.IN_PROGRESS, canRecord = true),
        messages = previewMessages + previewWorkRecord,
        recordRequested = true,
    )

@Composable
private fun ConversationPreviewFrame(uiState: ConversationUiState) {
    SaludEnCasaTheme {
        ConversationContent(uiState = uiState, today = previewToday, actions = ConversationActions())
    }
}

@Preview(name = "Conversation", showBackground = true)
@Composable
private fun ConversationPreview() {
    ConversationPreviewFrame(previewContent)
}

@Preview(
    name = "Conversation dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ConversationDarkPreview() {
    ConversationPreviewFrame(previewContent)
}

@Preview(name = "Conversation large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun ConversationLargeFontPreview() {
    ConversationPreviewFrame(previewContent)
}

@Preview(name = "Conversation empty", showBackground = true)
@Composable
private fun ConversationEmptyPreview() {
    ConversationPreviewFrame(previewContent.copy(messages = emptyList()))
}

@Preview(name = "Conversation closed", showBackground = true)
@Composable
private fun ConversationClosedPreview() {
    ConversationPreviewFrame(
        previewContent.copy(header = previewHeader.copy(status = ServiceStatus.CANCELLED, canWrite = false)),
    )
}

@Preview(name = "Conversation not sent", showBackground = true)
@Composable
private fun ConversationNotSentPreview() {
    ConversationPreviewFrame(
        previewContent.copy(
            older = OlderMessages.LOADING,
            outbox =
                listOf(
                    ConversationItem.NotSent("n1", "Muchas gracias por todo.", MessageError.NetworkUnavailable),
                ),
        ),
    )
}

@Preview(name = "Conversation recording", showBackground = true)
@Composable
private fun ConversationRecordingPreview() {
    ConversationPreviewFrame(previewRecording)
}

@Preview(
    name = "Conversation recording dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ConversationRecordingDarkPreview() {
    ConversationPreviewFrame(previewRecording)
}

@Preview(name = "Conversation recording large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun ConversationRecordingLargeFontPreview() {
    ConversationPreviewFrame(previewRecording.copy(messages = listOf(previewWorkRecord)))
}

@Preview(name = "Conversation recording student", showBackground = true)
@Composable
private fun ConversationRecordingStudentPreview() {
    ConversationPreviewFrame(
        previewRecording.copy(
            header = previewRecording.header.copy(showsStudentNotice = true),
        ),
    )
}

@Preview(name = "Conversation recording student large font", showBackground = true, fontScale = 2f, widthDp = 320)
@Composable
private fun ConversationRecordingStudentLargeFontPreview() {
    ConversationPreviewFrame(
        previewRecording.copy(
            header = previewRecording.header.copy(showsStudentNotice = true),
            messages = listOf(previewWorkRecord),
        ),
    )
}

@Preview(name = "Conversation record as the patient", showBackground = true)
@Composable
private fun ConversationRecordAsThePatientPreview() {
    ConversationPreviewFrame(
        previewContent.copy(
            header = previewHeader.copy(counterpartName = "María Flores", status = ServiceStatus.COMPLETED),
            messages = previewMessages + previewWorkRecord.copy(sentByMe = false),
        ),
    )
}

@Preview(name = "Conversation record not allowed", showBackground = true)
@Composable
private fun ConversationRecordNotAllowedPreview() {
    ConversationPreviewFrame(
        previewContent.copy(
            header = previewHeader.copy(status = ServiceStatus.IN_PROGRESS, canRecord = true),
            outbox =
                listOf(
                    ConversationItem.NotSent(
                        key = "n1",
                        text = "Curación de la herida de la pierna derecha.",
                        error = MessageError.WorkRecordNotAllowed,
                        isWorkRecord = true,
                    ),
                ),
        ),
    )
}

@Preview(name = "Conversation failed", showBackground = true)
@Composable
private fun ConversationFailedPreview() {
    ConversationPreviewFrame(ConversationUiState.Failed(MessageError.NetworkUnavailable))
}
