package ai.saniou.thread.network

import de.jensklingenberg.ktorfit.converter.Converter

class WireConverterFactory : Converter.Factory {

//    override fun responseConverter(
//        typeData: TypeData,
//        ktorfit: Ktorfit,
//    ): Converter.ResponseConverter<HttpResponse, *>? {
//        val adapter = WireUtils.getAdapter(typeData.typeInfo.type)
//        if (adapter != null) {
//            return object : Converter.ResponseConverter<HttpResponse, Any> {
//                override fun convert(getResponse: suspend () -> HttpResponse): Any {
//                    val response = getResponse()
//                    val byteArray = response.bodyAsChannel().toByteArray()
//                    return adapter.decode(byteArray)
//                }
//            }
//        }
//        return null
//    }

}
