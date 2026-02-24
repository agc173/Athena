package com.agc.bwitch.ui.userprofile

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@Composable
actual fun AvatarPickerButton(
    enabled: Boolean,
    onPicked: (uriString: String, mimeType: String?) -> Unit
) {
    val delegate = remember { ImagePickerDelegate(onPicked) }

    Button(
        onClick = {
            if (!enabled) return@Button

            val picker = UIImagePickerController().apply {
                sourceType = platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                allowsEditing = false
                modalPresentationStyle = UIModalPresentationFullScreen
                this.delegate = delegate
            }

            topViewController()?.presentViewController(picker, animated = true, completion = null)
        },
        enabled = enabled
    ) {
        Text("Seleccionar avatar")
    }
}

private class ImagePickerDelegate(
    private val onPicked: (uriString: String, mimeType: String?) -> Unit
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[platform.UIKit.UIImagePickerControllerOriginalImage] as? UIImage
        if (image != null) {
            val fileUrl = saveImageToTempJpeg(image)
            if (fileUrl != null) {
                onPicked(fileUrl.absoluteString ?: fileUrl.toString(), "image/jpeg")
            }
        }
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    private fun saveImageToTempJpeg(image: UIImage): NSURL? {
        val data: NSData? = UIImageJPEGRepresentation(image, 0.9)
        if (data == null) return null

        val filename = "avatar_${NSUUID().UUIDString}.jpg"
        val dir = NSTemporaryDirectory().trimEnd('/')
        val path = "$dir/$filename"
        val url = NSURL.fileURLWithPath(path)

        val ok = data.writeToURL(url, atomically = true)
        return if (ok) url else null
    }
}

private fun topViewController(): UIViewController? {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    return root.topMost()
}

private fun UIViewController.topMost(): UIViewController {
    var top = this
    while (true) {
        val presented = top.presentedViewController ?: break
        top = presented
    }
    return top
}