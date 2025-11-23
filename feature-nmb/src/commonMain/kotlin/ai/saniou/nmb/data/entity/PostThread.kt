package ai.saniou.nmb.data.entity


/**
 * 返回的数据
 * 请求成功
 * HTML 响应内容示例
 *
 * <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
 * <html xmlns="http://www.w3.org/1999/xhtml">
 * <head>
 *     <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
 *     <title>跳转提示</title>
 *     <style type="text/css">/* ... */</style>
 *     <meta name="__hash__" content="..." />
 * </head>
 * <body>
 *     <div class="system-message">
 *         <h1>:(</h1>
 *         <p class="success">发串成功</p>
 *         <p class="detail"></p>
 *         <p class="jump">
 *             页面自动 <a id="href" href="https://www.nmbxd.com/home/forum/doPostThread.html">跳转</a> 等待时间： <b id="wait">3</b>
 *         </p>
 *     </div>
 *     <script type="text/javascript">
 *         (function(){
 *             var wait = document.getElementById('wait'),href = document.getElementById('href').href;
 *             var interval = setInterval(function(){
 *                 var time = --wait.innerHTML;
 *                 if(time <= 0) {
 *                     location.href = href;
 *                     clearInterval(interval);
 *                 };
 *             }, 1000);
 *         })();
 *     </script>
 * </body>
 * </html>
 *
 *请求错误
 * HTML 响应内容示例
 *
 * <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
 * <html xmlns="http://www.w3.org/1999/xhtml">
 * <head>
 *     <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
 *     <title>跳转提示</title>
 *     <style type="text/css">/* ... */</style>
 *     <meta name="__hash__" content="..." />
 * </head>
 * <body>
 *     <div class="system-message">
 *         <h1>:(</h1>
 *         <p class="error">未应用饼干，请在用户中心应用饼干</p>
 *         <p class="detail"></p>
 *         <p class="jump">
 *             页面自动 <a id="href" href="/Member/User/Cookie/index.html">跳转</a> 等待时间： <b id="wait">3</b>
 *         </p>
 *     </div>
 *     <script type="text/javascript">
 *         (function(){
 *             var wait = document.getElementById('wait'),href = document.getElementById('href').href;
 *             var interval = setInterval(function(){
 *                 var time = --wait.innerHTML;
 *                 if(time <= 0) {
 *                     location.href = href;
 *                     clearInterval(interval);
 *                 };
 *             }, 1000);
 *         })();
 *     </script>
 * </body>
 * </html>
 *
 *
 */
data class PostThreadRequest(
    val name: String? = null,// 默认为“无名氏”的发串人名称，可选，当然实际上用这个来自爆身份的话是会被肥肥们 (　^ω^) 的
    val title: String? = null,// 默认为“无标题”的串标题，可选
    val content: String? = null,// 串的内容
    val fid: Int? = null,// 发串的版面 ID
//    val image: File?=null,// 附加图片，可选
    val water: Boolean? = null,// 附加图片是否添加水印，可选
)
