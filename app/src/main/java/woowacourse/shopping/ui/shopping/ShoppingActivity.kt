package woowacourse.shopping.ui.shopping

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import woowacourse.shopping.R
import woowacourse.shopping.common.utils.convertDpToPixel
import woowacourse.shopping.common.utils.getSerializable
import woowacourse.shopping.data.cart.CartRepositoryImpl
import woowacourse.shopping.data.database.ShoppingDBOpenHelper
import woowacourse.shopping.data.database.dao.CartDao
import woowacourse.shopping.data.database.dao.ProductDao
import woowacourse.shopping.data.database.dao.RecentProductDao
import woowacourse.shopping.data.product.ProductRepositoryImpl
import woowacourse.shopping.data.recentproduct.RecentProductRepositoryImpl
import woowacourse.shopping.data.server.ProductRemoteDataSourceImpl
import woowacourse.shopping.databinding.ActivityShoppingBinding
import woowacourse.shopping.ui.cart.CartActivity
import woowacourse.shopping.ui.model.ProductModel
import woowacourse.shopping.ui.model.RecentProductModel
import woowacourse.shopping.ui.model.ShoppingProductModel
import woowacourse.shopping.ui.productdetail.ProductDetailActivity
import woowacourse.shopping.ui.shopping.recyclerview.LoadMoreAdapter
import woowacourse.shopping.ui.shopping.recyclerview.ProductAdapter
import woowacourse.shopping.ui.shopping.recyclerview.RecentProductAdapter
import woowacourse.shopping.ui.shopping.recyclerview.RecentProductWrapperAdapter

class ShoppingActivity : AppCompatActivity(), ShoppingContract.View {
    private lateinit var binding: ActivityShoppingBinding
    private lateinit var presenter: ShoppingContract.Presenter
    private var shoppingCartAmount: TextView? = null

    private val productAdapter: ProductAdapter by lazy {
        ProductAdapter(
            onProductItemClick = { presenter.openProduct(it.product) },
            onMinusAmountButtonClick = { presenter.decreaseCartProductAmount(it) },
            onPlusAmountButtonClick = { presenter.increaseCartProductAmount(it) }
        )
    }

    private val recentProductAdapter: RecentProductAdapter by lazy {
        RecentProductAdapter(emptyList())
    }

    private val recentProductWrapperAdapter: RecentProductWrapperAdapter by lazy {
        RecentProductWrapperAdapter(recentProductAdapter)
    }

    private val loadMoreAdapter: LoadMoreAdapter by lazy {
        LoadMoreAdapter {
            presenter.loadMoreProduct()
        }
    }

    private val concatAdapter: ConcatAdapter by lazy {
        val config = ConcatAdapter.Config.Builder().apply {
            setIsolateViewTypes(false)
        }.build()
        ConcatAdapter(
            config, recentProductWrapperAdapter, productAdapter, loadMoreAdapter
        )
    }

    private val gridItemDecoration = object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view) - 1
            val column = position % SPAN_COUNT
            val density = resources.displayMetrics.density

            if (position in 0 until SPAN_COUNT) {
                outRect.top += convertDpToPixel(DP_GRID_TOP_OFFSET, density)
            }

            val edgeHorizontalOffset = convertDpToPixel(DP_GRID_EDGE_HORIZONTAL_OFFSET, density)
            if (column == 0) {
                outRect.left += edgeHorizontalOffset
            } else if (column == SPAN_COUNT - 1) {
                outRect.right += edgeHorizontalOffset
            }
        }
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val difference: ArrayList<ShoppingProductModel>? = result.data?.getSerializable(EXTRA_KEY_DIFFERENCE)
            val amountDifference = result.data?.getIntExtra(EXTRA_KEY_AMOUNT_DIFFERENCE, 0) ?: 0
            updateChange(difference, amountDifference)
        }
    }

    private fun updateChange(
        difference: ArrayList<ShoppingProductModel>?,
        amountDifference: Int
    ) {
        if (difference != null) {
            presenter.updateChange(difference.toList())
            updateCartAmount(shoppingCartAmount?.text.toString().toInt() + amountDifference)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.shopping_toolbar))

        binding.shoppingProductList.itemAnimator = null

        initProductList()

        initPresenter()
    }

    override fun onResume() {
        super.onResume()
        presenter.updateRecentProducts()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_shopping, menu)
        val shoppingCartAction = menu?.findItem(R.id.shopping_cart_action)
        shoppingCartAction?.actionView?.setOnClickListener {
            onOptionsItemSelected(shoppingCartAction)
        }

        shoppingCartAmount =
            shoppingCartAction?.actionView?.findViewById(R.id.tv_shopping_cart_amount)
        presenter.setUpCartAmount()

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.shopping_cart_action -> presenter.openCart()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun updateProducts(productModels: List<ShoppingProductModel>) {
        productAdapter.updateProducts(productModels)
    }

    override fun addProducts(productModels: List<ShoppingProductModel>) {
        productAdapter.addProducts(productModels)
    }

    override fun updateRecentProducts(recentProductModels: List<RecentProductModel>) {
        recentProductAdapter.updateRecentProducts(recentProductModels)
        recentProductWrapperAdapter.updateRecentProduct()
    }

    override fun showProductDetail(productModel: ProductModel, recentProductModel: ProductModel?) {
        startProductDetailActivity(productModel, recentProductModel)
    }

    override fun showCart() {
        startCartActivity()
    }

    override fun updateCartAmount(amount: Int) {
        shoppingCartAmount?.text = amount.toString()
    }

    override fun updateShoppingProduct(prev: ShoppingProductModel, new: ShoppingProductModel) {
        productAdapter.updateProduct(prev, new)
    }

    override fun updateChange(difference: List<ShoppingProductModel>) {
        productAdapter.updateChange(difference)
    }

    private fun startCartActivity() {
        val intent = CartActivity.createIntent(this)
        activityResultLauncher.launch(intent)
    }

    private fun initProductList() {
        binding.shoppingProductList.layoutManager = makeLayoutManager()
        binding.shoppingProductList.addItemDecoration(gridItemDecoration)
        binding.shoppingProductList.adapter = concatAdapter
    }

    private fun initPresenter() {
        val db = ShoppingDBOpenHelper(this).writableDatabase
        val productRepository = ProductRepositoryImpl(
            productDao = ProductDao(db),
            productRemoteDataSource = ProductRemoteDataSourceImpl()
        )
        presenter = ShoppingPresenter(
            this,
            productRepository = productRepository,
            recentProductRepository = RecentProductRepositoryImpl(RecentProductDao(db)),
            cartRepository = CartRepositoryImpl(CartDao(db)),
            recentProductSize = 10,
            productLoadSize = 20
        )
    }

    private fun makeLayoutManager(): GridLayoutManager {
        return GridLayoutManager(this, SPAN_COUNT).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (concatAdapter.getItemViewType(position)) {
                        ProductAdapter.VIEW_TYPE -> 1
                        RecentProductWrapperAdapter.VIEW_TYPE -> 2
                        else -> 2
                    }
                }
            }
        }
    }

    private fun startProductDetailActivity(
        productModel: ProductModel,
        recentProductModel: ProductModel?
    ) {
        val intent = ProductDetailActivity.createIntent(
            this,
            productModel,
            recentProductModel,
            emptyList(),
            0
        )
        activityResultLauncher.launch(intent)
    }

    companion object {
        private const val SPAN_COUNT = 2
        private const val DP_GRID_TOP_OFFSET = 10
        private const val DP_GRID_EDGE_HORIZONTAL_OFFSET = 14
        private const val EXTRA_KEY_DIFFERENCE = "difference"
        private const val EXTRA_KEY_AMOUNT_DIFFERENCE = "amountDifference"

        fun createIntent(
            context: Context,
            difference: List<ShoppingProductModel>,
            amountDifference: Int
        ): Intent {
            val intent = Intent(context, ShoppingActivity::class.java)
            intent.putExtra(EXTRA_KEY_DIFFERENCE, ArrayList(difference))
            intent.putExtra(EXTRA_KEY_AMOUNT_DIFFERENCE, amountDifference)
            return intent
        }
    }
}