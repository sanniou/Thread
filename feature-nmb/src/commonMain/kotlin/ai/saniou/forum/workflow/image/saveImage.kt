package ai.saniou.forum.workflow.image

import org.kodein.di.DI


/**
 * 桌面平台保存图片实现
 */
expect suspend fun ImagePreviewViewModelParams.saveImage(
    di: DI,
    imgPath: String,
    ext: String
)
