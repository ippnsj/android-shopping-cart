package woowacourse.shopping.productdetail.dialog

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import woowacourse.shopping.domain.repository.CartRepository

class CartProductDialogPresenterTest {
    private lateinit var presenter: CartProductDialogPresenter
    private lateinit var view: CartProductDialogContract.View
    private lateinit var cartRepository: CartRepository

    @Before
    fun setUp() {
        view = mockk(relaxed = true)
        cartRepository = mockk(relaxed = true)
    }

    @Test
    fun 프레젠터를_생성하면_뷰에_카트_상품_수량이_업데이트_된다() {
        // given

        // when
        presenter = CartProductDialogPresenter(
            view,
            productModel = mockk(relaxed = true),
            cartRepository,
            cartProductAmount = 1
        )

        // then
        verify(exactly = 1) { view.updateCartProductAmount(any()) }
    }

    @Test
    fun 카트_상품_수량이_1보다_클_때_수량을_감소시키면_뷰에_카트_상품_수량이_업데이트_된다() {
        // given
        presenter = CartProductDialogPresenter(
            view,
            productModel = mockk(relaxed = true),
            cartRepository,
            cartProductAmount = 2
        )

        // when
        presenter.decreaseCartProductAmount()

        // then
        verify(exactly = 2) { view.updateCartProductAmount(any()) }
    }

    @Test
    fun 카트_상품_수량이_1_이하일_때_수량을_감소시키면_뷰에_카트_상품_수량이_업데이트_되지_않는다() {
        // given
        presenter = CartProductDialogPresenter(
            view,
            productModel = mockk(relaxed = true),
            cartRepository,
            cartProductAmount = 1
        )

        // when
        presenter.decreaseCartProductAmount()

        // then
        verify(exactly = 1) { view.updateCartProductAmount(any()) }
    }

    @Test
    fun 카트_상품_수량을_증가시키면_뷰에_카트_상품_수량이_업데이트_된다() {
        // given
        presenter = CartProductDialogPresenter(
            view,
            productModel = mockk(relaxed = true),
            cartRepository,
            cartProductAmount = 1
        )

        // when
        presenter.increaseCartProductAmount()

        // then
        verify(exactly = 2) { view.updateCartProductAmount(any()) }
    }
}
