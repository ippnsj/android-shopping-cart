package woowacourse.shopping.common.model

import java.io.Serializable

data class CartProductModel(
    val ordinal: Int,
    val product: ProductModel
) : Serializable