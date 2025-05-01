package ai.saniou.nmb.workflow.image

import org.kodein.di.DI


/**
 * 桌面平台保存图片实现
 */
expect suspend fun ImagePreviewViewModel.saveImage(
    di: DI,
    imgPath: String,
    ext: String
)
