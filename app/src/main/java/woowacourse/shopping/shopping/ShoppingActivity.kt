package woowacourse.shopping.shopping

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import woowacourse.shopping.R
import woowacourse.shopping.cart.CartActivity
import woowacourse.shopping.common.data.database.ShoppingDBOpenHelper
import woowacourse.shopping.common.data.database.dao.ProductDao
import woowacourse.shopping.common.data.database.dao.RecentProductDao
import woowacourse.shopping.common.model.ProductModel
import woowacourse.shopping.common.model.RecentProductModel
import woowacourse.shopping.databinding.ActivityShoppingBinding
import woowacourse.shopping.productdetail.ProductDetailActivity

class ShoppingActivity : AppCompatActivity(), ShoppingContract.View {
    private lateinit var binding: ActivityShoppingBinding
    private lateinit var presenter: ShoppingContract.Presenter
    private lateinit var productAdapter: ProductAdapter
    private lateinit var recentProductAdapter: RecentProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.shopping_toolbar))

        initProductAdapter()
        initRecentProductAdapter()

        initPresenter()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_shopping, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.shopping_cart_action -> presenter.openCart()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun updateProductList(productModels: List<ProductModel>) {
        productAdapter.updateProducts(productModels)
    }

    override fun updateRecentProductList(recentProductModels: List<RecentProductModel>) {
        println(recentProductModels.size)
        binding.shoppingRecentProductLayout.visibility =
            if (recentProductModels.isEmpty()) View.GONE
            else View.VISIBLE

        recentProductAdapter.updateRecentProducts(recentProductModels)
    }

    override fun showProductDetail(productModel: ProductModel) {
        startProductDetailActivity(productModel)
    }

    override fun showCart() {
        startCartActivity()
    }

    private fun startCartActivity() {
        val intent = CartActivity.createIntent(this)
        startActivity(intent)
    }

    private fun initPresenter() {
        deleteDatabase(ShoppingDBOpenHelper.DB_NAME)
        val db = ShoppingDBOpenHelper(this).writableDatabase
        val productDao = ProductDao(db)
        repeat(10) {
            productDao.insertProduct(createProductMock())
        }

        val recentProductDao = RecentProductDao(db)
        repeat(10) {
            recentProductDao.insertRecentProduct(createRecentProductMock())
        }
        presenter = ShoppingPresenter(
            this,
            productDao = productDao,
            recentProductDao = recentProductDao,
            recentProductSize = 10
        )
    }

    fun createProductMock() = ProductModel(
        "https://blog.kakaocdn.net/dn/bmaMSZ/btqHq1wiJXa/tkGODWI7E0pvCf8NnA8Kp1/img.png",
        "asdfasdfasdfasdfasdfasdf",
        100000
    )

    var ordinal = 1

    fun createRecentProductMock() = RecentProductModel(
        ordinal++,
        createProductMock()
    )
    private fun initProductAdapter() {
        productAdapter = ProductAdapter(
            emptyList(),
            onProductItemClick = { startProductDetailActivity(it) }
        )
        binding.shoppingProductList.layoutManager = GridLayoutManager(this, 2)
        binding.shoppingProductList.adapter = productAdapter
    }

    private fun startProductDetailActivity(productModel: ProductModel) {
        val intent = ProductDetailActivity.createIntent(this, productModel)
        startActivity(intent)
    }

    private fun initRecentProductAdapter() {
        recentProductAdapter = RecentProductAdapter(emptyList())
        binding.shoppingRecentProductList.adapter = recentProductAdapter
    }
}
