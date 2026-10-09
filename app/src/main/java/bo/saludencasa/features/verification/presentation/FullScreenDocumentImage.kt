package bo.saludencasa.features.verification.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import bo.saludencasa.R
import bo.saludencasa.ui.theme.SaludEnCasaTheme
import bo.saludencasa.ui.theme.Spacing
import coil3.compose.SubcomposeAsyncImage

internal const val MIN_DOCUMENT_ZOOM = 1f
internal const val MAX_DOCUMENT_ZOOM = 5f
internal const val DOUBLE_TAP_DOCUMENT_ZOOM = 2.5f

internal data class ImageTransform(
    val scale: Float = MIN_DOCUMENT_ZOOM,
    val offset: Offset = Offset.Zero,
) {
    val isFitted: Boolean get() = scale <= MIN_DOCUMENT_ZOOM
}

// The pan has to stay inside the enlarged image or a flick drags the document
// off the screen and nothing brings it back. At scale s the content measures the
// container times s, so it can travel half that surplus in each direction, and
// at the fitted scale the surplus is zero and the image recentres itself.
internal fun ImageTransform.transformedBy(
    zoom: Float,
    pan: Offset,
    container: Size,
): ImageTransform {
    val scaled = (scale * zoom).coerceIn(MIN_DOCUMENT_ZOOM, MAX_DOCUMENT_ZOOM)
    if (scaled == MIN_DOCUMENT_ZOOM) return ImageTransform(scale = scaled)
    // The offset grows with the zoom so the point under the fingers stays where
    // it was instead of sliding toward the centre.
    val moved = offset * (scaled / scale) + pan
    val limitX = container.width * (scaled - MIN_DOCUMENT_ZOOM) / 2f
    val limitY = container.height * (scaled - MIN_DOCUMENT_ZOOM) / 2f
    return ImageTransform(
        scale = scaled,
        offset = Offset(moved.x.coerceIn(-limitX, limitX), moved.y.coerceIn(-limitY, limitY)),
    )
}

@Composable
internal fun FullScreenDocumentImage(
    model: Any,
    onDismiss: () -> Unit,
) {
    // usePlatformDefaultWidth off: a dialog otherwise keeps the inset width of a
    // form, and the document needs the whole screen.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ZoomableDocumentImage(model = model, onDismiss = onDismiss)
    }
}

@Composable
internal fun ZoomableDocumentImage(
    model: Any,
    onDismiss: () -> Unit,
) {
    var transform by remember { mutableStateOf(ImageTransform()) }
    var container by remember { mutableStateOf(Size.Zero) }
    val loadingDescription = stringResource(R.string.cd_loading)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .onSizeChanged { container = it.toSize() },
    ) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = stringResource(R.string.cd_document_image),
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                transform =
                                    if (transform.isFitted) {
                                        ImageTransform(scale = DOUBLE_TAP_DOCUMENT_ZOOM)
                                    } else {
                                        ImageTransform()
                                    }
                            },
                        )
                    }.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            transform = transform.transformedBy(zoom, pan, container)
                        }
                    }.graphicsLayer(
                        scaleX = transform.scale,
                        scaleY = transform.scale,
                        translationX = transform.offset.x,
                        translationY = transform.offset.y,
                    ),
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = loadingDescription },
                    )
                }
            },
            // No retry here: the viewer only opens over a preview that already
            // loaded, so the way out of a failure is to close.
            error = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.review_image_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )

        // A filled button so it reads over the dark backdrop, which carries no
        // content colour of its own.
        FilledIconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.screenMargin),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_close_full_image),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 640, name = "Visor a pantalla completa")
@Composable
private fun ZoomableDocumentImageLightPreview() {
    SaludEnCasaTheme(darkTheme = false) {
        ZoomableDocumentImage(model = "", onDismiss = {})
    }
}

@Preview(showBackground = true, heightDp = 640, fontScale = 2f, name = "Visor al 200 %")
@Composable
private fun ZoomableDocumentImageLargeFontPreview() {
    SaludEnCasaTheme(darkTheme = true) {
        ZoomableDocumentImage(model = "", onDismiss = {})
    }
}
