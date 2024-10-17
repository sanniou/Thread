package ai.saniou.nmb.data.entity


data class PostThreadRequest(
    val name: String? = null,// 默认为“无名氏”的发串人名称，可选，当然实际上用这个来自爆身份的话是会被肥肥们 (　^ω^) 的
    val title: String? = null,// 默认为“无标题”的串标题，可选
    val content: String? = null,// 串的内容
    val fid: Int? = null,// 发串的版面 ID
//    val image: File?=null,// 附加图片，可选
    val water: Boolean? = null,// 附加图片是否添加水印，可选
)
