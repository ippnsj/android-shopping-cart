package woowacourse.shopping.ui.shopping

import woowacourse.shopping.domain.CartProduct
import woowacourse.shopping.domain.Product
import woowacourse.shopping.domain.RecentProduct
import woowacourse.shopping.domain.repository.CartRepository
import woowacourse.shopping.domain.repository.ProductRepository
import woowacourse.shopping.domain.repository.RecentProductRepository
import woowacourse.shopping.ui.model.ProductModel
import woowacourse.shopping.ui.model.ShoppingProductModel
import woowacourse.shopping.ui.model.mapper.ProductMapper.toDomain
import woowacourse.shopping.ui.model.mapper.ProductMapper.toView
import woowacourse.shopping.ui.model.mapper.RecentProductMapper.toView
import woowacourse.shopping.ui.model.mapper.ShoppingProductMapper.toView

class ShoppingPresenter(
    private val view: ShoppingContract.View,
    private val productRepository: ProductRepository,
    private val recentProductRepository: RecentProductRepository,
    private val cartRepository: CartRepository,
    private val recentProductSize: Int,
    private val productLoadSize: Int,
) : ShoppingContract.Presenter {
    private var productSize: Int = 0

    init {
        loadMoreProduct()
    }

    override fun updateChange(difference: List<ShoppingProductModel>) {
        view.updateChange(difference)
    }

    override fun updateRecentProducts() {
        val recentProducts = recentProductRepository.getAll()
        view.updateRecentProducts(recentProducts.getRecentProducts(recentProductSize).value.map { it.toView() })
    }

    override fun setUpCartAmount() {
        updateCartAmount()
    }

    override fun openProduct(productModel: ProductModel) {
        val latestRecentProduct = recentProductRepository.getLatestRecentProduct()
        updateRecentProducts(productModel)

        if (productModel.toDomain().isLatestRecentProduct(latestRecentProduct)) {
            view.showProductDetail(productModel, null)
        } else {
            view.showProductDetail(productModel, latestRecentProduct?.product?.toView())
        }
    }

    private fun Product.isLatestRecentProduct(latestRecentProduct: RecentProduct?) =
        latestRecentProduct?.product == this

    private fun updateRecentProducts(productModel: ProductModel) {
        val recentProducts = recentProductRepository.getAll()
        var recentProduct = recentProductRepository.getByProduct(productModel.toDomain())

        if (recentProduct == null) {
            recentProduct = recentProducts.makeRecentProduct(productModel.toDomain())
            addRecentProduct(recentProduct)
        } else {
            recentProduct = recentProduct.updateTime()
            updateRecentProduct(recentProduct)
        }
    }

    private fun addRecentProduct(recentProduct: RecentProduct) {
        recentProductRepository.addRecentProduct(recentProduct)
    }

    private fun updateRecentProduct(recentProduct: RecentProduct) {
        recentProductRepository.modifyRecentProduct(recentProduct)
    }

    override fun openCart() {
        view.showCart()
    }

    override fun loadMoreProduct() {
        val loadedProducts = productRepository.getProducts(productSize, productLoadSize)
        productSize += loadedProducts.value.size
        view.addProducts(loadedProducts.value.map { it.toView() })
    }

    private fun updateCartAmount() {
        val totalAmount = cartRepository.getTotalAmount()
        view.updateCartAmount(totalAmount)
    }

    override fun decreaseCartProductAmount(shoppingProductModel: ShoppingProductModel) {
        var cartProduct = getCartProduct(shoppingProductModel.product)
        cartProduct = cartProduct.decreaseAmount()
        if (cartProduct.amount > 0) {
            updateCartProduct(cartProduct)
        } else {
            removeFromCart(cartProduct)
        }
        updateShoppingProduct(shoppingProductModel, cartProduct)
        updateCartAmount()
    }

    private fun removeFromCart(cartProduct: CartProduct) {
        cartRepository.deleteCartProduct(cartProduct)
    }

    override fun increaseCartProductAmount(shoppingProductModel: ShoppingProductModel) {
        var cartProduct = getCartProduct(shoppingProductModel.product)
        cartProduct = cartProduct.increaseAmount()
        if (cartProduct.amount > 1) {
            updateCartProduct(cartProduct)
        } else {
            addToCart(cartProduct)
        }
        updateShoppingProduct(shoppingProductModel, cartProduct)
        updateCartAmount()
    }

    private fun getCartProduct(productModel: ProductModel): CartProduct {
        return cartRepository.getCartProductByProduct(productModel.toDomain())
    }

    private fun updateCartProduct(cartProduct: CartProduct) {
        cartRepository.modifyCartProduct(cartProduct)
    }

    private fun addToCart(cartProduct: CartProduct) {
        cartRepository.addCartProduct(cartProduct)
    }

    private fun updateShoppingProduct(
        shoppingProductModel: ShoppingProductModel,
        cartProduct: CartProduct
    ) {
        val newShoppingProductModel = ShoppingProductModel(
            shoppingProductModel.product,
            cartProduct.amount
        )
        view.updateShoppingProduct(shoppingProductModel, newShoppingProductModel)
    }
}