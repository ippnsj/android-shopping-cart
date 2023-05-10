package woowacourse.shopping.productdetail

import woowacourse.shopping.common.data.database.dao.CartDao
import woowacourse.shopping.common.data.database.state.CartState
import woowacourse.shopping.common.data.database.state.State
import woowacourse.shopping.common.model.mapper.CartProductMapper.toView
import woowacourse.shopping.common.model.mapper.ProductMapper.toView
import woowacourse.shopping.domain.Cart
import woowacourse.shopping.domain.Product

class ProductDetailPresenter(
    private val view: ProductDetailContract.View,
    private val product: Product,
    private var cart: Cart = Cart(emptyList()),
    private val cartState: State<Cart> = CartState,
    private val cartDao: CartDao
) : ProductDetailContract.Presenter {
    init {
        view.updateProductDetail(product.toView())
    }

    override fun addToCart() {
        cart = cartState.load()
        val cartProduct = cart.makeCartProduct(product)
        cartState.save(cart.add(cartProduct))
        cartDao.insertCartProduct(cartProduct.toView())
        view.showCart()
    }
}