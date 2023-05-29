package woowacourse.shopping.ui.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import woowacourse.shopping.databinding.ItemCartNavigatorBinding
import woowacourse.shopping.databinding.ItemCartProductListBinding
import woowacourse.shopping.ui.model.CartProductModel

class CartAdapter(
    cartProductListener: CartProductListener,
    private val cartNavigationListener: CartNavigationListener,
    private var currentPage: Int = 1,
    private var isNavigationVisible: Boolean = false,
    private var isLastPage: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val cartProducts: MutableList<CartProductModel> = mutableListOf()
    private val onCartItemRemoveButtonViewClick: (Int) -> Unit =
        { cartProductListener.onCartItemRemoveButtonClick(cartProducts[it]) }
    private val onCheckBoxViewClick: (Int) -> Unit =
        { cartProductListener.onCheckBoxClick(cartProducts[it]) }
    private val onMinusAmountButtonViewClick: (Int) -> Unit =
        { cartProductListener.onMinusAmountButtonClick(cartProducts[it]) }
    private val onPlusAmountButtonViewClick: (Int) -> Unit =
        { cartProductListener.onPlusAmountButtonClick(cartProducts[it]) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (CartViewType.values()[viewType]) {
            CartViewType.CART -> CartViewHolder(
                binding = ItemCartProductListBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                onCartItemRemoveButtonViewClick,
                onCheckBoxViewClick,
                onMinusAmountButtonViewClick,
                onPlusAmountButtonViewClick
            )
            CartViewType.NAVIGATION -> NavigationViewHolder(
                ItemCartNavigatorBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                cartNavigationListener
            )
        }
    }

    override fun getItemCount(): Int = cartProducts.size + if (isNavigationVisible) 1 else 0

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (CartViewType.values()[getItemViewType(position)]) {
            CartViewType.CART -> (holder as CartViewHolder).bind(cartProducts[position])
            CartViewType.NAVIGATION -> (holder as NavigationViewHolder).bind(
                currentPage,
                isLastPage
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position < cartProducts.size) return CartViewType.CART.ordinal
        return CartViewType.NAVIGATION.ordinal
    }

    fun updateCartProducts(
        cartProducts: List<CartProductModel>,
        currentPage: Int,
        isLastPage: Boolean
    ) {
        this.cartProducts.clear()
        this.cartProducts.addAll(cartProducts)
        this.currentPage = currentPage
        this.isLastPage = isLastPage
        notifyDataSetChanged()
    }

    fun updateNavigationVisible(isNavigationVisible: Boolean) {
        this.isNavigationVisible = isNavigationVisible
    }

    fun updateCartProduct(prev: CartProductModel, new: CartProductModel) {
        val index = cartProducts.indexOf(prev)
        cartProducts[index] = new
        notifyItemChanged(index)
    }
}