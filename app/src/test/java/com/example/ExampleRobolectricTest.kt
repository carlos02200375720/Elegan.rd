package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Elegan", appName)
  }

  @Test
  fun `test register and login local user`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val email = "test.cliente@gmail.com"
    val password = "Password123!"

    val regResponse = com.example.data.remote.MobileSession.registerLocalUser(
      context = context,
      name = "Cliente Prueba",
      email = email,
      password = password
    )
    assertEquals(email, regResponse.user.email)
    assertEquals("customer", regResponse.user.role)

    val loginResponse = com.example.data.remote.MobileSession.loginLocalUser(
      context = context,
      email = email,
      password = password
    )
    assertEquals(email, loginResponse.user.email)
    assertEquals("Cliente Prueba", loginResponse.user.name)
  }

  @Test
  fun `test super admin login gets admin role`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val adminResponse = com.example.data.remote.MobileSession.loginLocalUser(
      context = context,
      email = "cg0220037@gmail.com",
      password = "EleganAdmin2026!"
    )
    assertEquals("admin", adminResponse.user.role)
    assertEquals("cg0220037@gmail.com", adminResponse.user.email)
  }

  @Test
  fun `test launch MainActivity`() {
    val controller = org.robolectric.Robolectric.buildActivity(com.example.MainActivity::class.java)
    controller.setup()
    org.junit.Assert.assertNotNull(controller.get())
  }

  @Test
  fun `test remoteBearerToken ignores local guest and rejected tokens`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.remote.MobileSession.clear(context)

    com.example.data.remote.MobileSession.loginClientLocally(context)
    org.junit.Assert.assertNull(com.example.data.remote.MobileSession.remoteBearerToken(context))
    org.junit.Assert.assertEquals(false, com.example.data.remote.MobileSession.hasRemoteSession(context))

    val fakeJwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.sig"
    com.example.data.remote.MobileSession.save(
      context,
      com.example.data.remote.AuthResponse(
        token = fakeJwt,
        user = com.example.data.remote.AuthUser(id = "u1", name = "Cliente", role = "customer")
      )
    )
    assertEquals("Bearer $fakeJwt", com.example.data.remote.MobileSession.remoteBearerToken(context))

    com.example.data.remote.MobileSession.markRemoteSessionRejected()
    org.junit.Assert.assertNull(com.example.data.remote.MobileSession.remoteBearerToken(context))
    com.example.data.remote.MobileSession.clear(context)
  }

  @Test
  fun `test saved products and publication save counter persist across repository refresh`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.remote.MobileSession.clear(context)
    com.example.data.remote.MobileSession.loginClientLocally(context)

    val repo1 = com.example.data.repository.EleganRepository(context)
    repo1.setProductSaved("prod_test_1", true)

    val saved1 = repo1.observeSavedProductIds().first()
    val stats1 = repo1.observePublicationStats().first()
    org.junit.Assert.assertTrue(saved1.contains("prod_test_1"))
    assertEquals(1L, stats1["prod_test_1"]?.savesCount)

    // Simular reinicio / recarga con una nueva instancia del repositorio
    val repo2 = com.example.data.repository.EleganRepository(context)
    val saved2 = repo2.observeSavedProductIds().first()
    val stats2 = repo2.observePublicationStats().first()
    org.junit.Assert.assertTrue(saved2.contains("prod_test_1"))
    assertEquals(1L, stats2["prod_test_1"]?.savesCount)

    // Quitar guardado y verificar persistencia del decremento en otra instancia nueva
    repo2.setProductSaved("prod_test_1", false)
    val repo3 = com.example.data.repository.EleganRepository(context)
    val saved3 = repo3.observeSavedProductIds().first()
    val stats3 = repo3.observePublicationStats().first()
    org.junit.Assert.assertFalse(saved3.contains("prod_test_1"))
    assertEquals(0L, stats3["prod_test_1"]?.savesCount ?: 0L)

    com.example.data.remote.MobileSession.clear(context)
  }

  @Test
  fun `test liked publications toggle and persist across repository refresh`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.remote.MobileSession.clear(context)
    com.example.data.remote.MobileSession.loginClientLocally(context)

    val repo1 = com.example.data.repository.EleganRepository(context)
    val likedAfterFirstTap = repo1.togglePublicationLike("prod_like_1")
    org.junit.Assert.assertTrue(likedAfterFirstTap)

    val likedIds1 = repo1.observeLikedPublicationIds().first()
    val stats1 = repo1.observePublicationStats().first()
    org.junit.Assert.assertTrue(likedIds1.contains("prod_like_1"))
    assertEquals(1L, stats1["prod_like_1"]?.likesCount)

    // Simular reinicio / recarga con nueva instancia
    val repo2 = com.example.data.repository.EleganRepository(context)
    val likedIds2 = repo2.observeLikedPublicationIds().first()
    val stats2 = repo2.observePublicationStats().first()
    org.junit.Assert.assertTrue(likedIds2.contains("prod_like_1"))
    assertEquals(1L, stats2["prod_like_1"]?.likesCount)

    // Quitar like y verificar persistencia del estado y decremento
    val likedAfterSecondTap = repo2.togglePublicationLike("prod_like_1")
    org.junit.Assert.assertFalse(likedAfterSecondTap)

    val repo3 = com.example.data.repository.EleganRepository(context)
    val likedIds3 = repo3.observeLikedPublicationIds().first()
    val stats3 = repo3.observePublicationStats().first()
    org.junit.Assert.assertFalse(likedIds3.contains("prod_like_1"))
    assertEquals(0L, stats3["prod_like_1"]?.likesCount ?: 0L)

    com.example.data.remote.MobileSession.clear(context)
  }

  @Test
  fun `test created order is saved and persists in order history across repository refresh`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.remote.MobileSession.clear(context)
    com.example.data.remote.MobileSession.loginClientLocally(context)

    val repo1 = com.example.data.repository.EleganRepository(context)
    val sampleProduct = repo1.getInitialSampleProducts().first()
    repo1.addToCart(sampleProduct, selectedSize = "M", quantity = 2L, selectedColor = "Negro")

    val orderId = repo1.createOrder(
      customerName = "Cliente Elegan",
      email = "cliente.elegan@gmail.com",
      phone = "809-555-0199",
      address = "Av. Winston Churchill 101",
      city = "Santo Domingo",
      paymentMethod = "Tarjeta",
      total = 110.0,
      itemCount = 2L,
      itemsSummary = "2x ${sampleProduct.name}"
    )

    val orders1 = repo1.observeOrders().first()
    val cart1 = repo1.observeCart().first()
    assertEquals(1, orders1.size)
    assertEquals(orderId, orders1.first().id)
    assertEquals("Cliente Elegan", orders1.first().customerName)
    assertEquals(110.0, orders1.first().total, 0.01)
    org.junit.Assert.assertTrue(cart1.isEmpty())

    // Simular reinicio / recarga y comprobar que el historial de pedidos conserva el pedido
    val repo2 = com.example.data.repository.EleganRepository(context)
    val orders2 = repo2.observeOrders().first()
    assertEquals(1, orders2.size)
    assertEquals(orderId, orders2.first().id)
    assertEquals("Av. Winston Churchill 101", orders2.first().address)

    com.example.data.remote.MobileSession.clear(context)
  }

  @Test
  fun `test toggle product stock and announcement active persist across repository refresh`() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      context.getSharedPreferences("elegan_publication_states", Context.MODE_PRIVATE).edit().clear().commit()
      com.example.data.remote.MobileSession.clear(context)

      val repo1 = com.example.data.repository.EleganRepository(context)
      val firstProduct = repo1.getInitialSampleProducts().first()
      val firstAnnouncement = repo1.getInitialSampleAnnouncements().first()

      repo1.toggleProductStock(firstProduct.id, false)
      repo1.toggleAnnouncementActive(firstAnnouncement.id, false)

      val products1 = repo1.observeProducts().first()
      val announcements1 = repo1.observeAnnouncements().first()
      org.junit.Assert.assertFalse(products1.first { it.id == firstProduct.id }.inStock)
      org.junit.Assert.assertFalse(announcements1.first { it.id == firstAnnouncement.id }.isActive)

      // Simular recarga con una nueva instancia del repositorio
      val repo2 = com.example.data.repository.EleganRepository(context)
      val products2 = repo2.observeProducts().first()
      val announcements2 = repo2.observeAnnouncements().first()
      org.junit.Assert.assertFalse(products2.first { it.id == firstProduct.id }.inStock)
      org.junit.Assert.assertFalse(announcements2.first { it.id == firstAnnouncement.id }.isActive)

      // Volver a encender y verificar actualización
      repo2.toggleProductStock(firstProduct.id, true)
      repo2.toggleAnnouncementActive(firstAnnouncement.id, true)
      val repo3 = com.example.data.repository.EleganRepository(context)
      org.junit.Assert.assertTrue(repo3.observeProducts().first().first { it.id == firstProduct.id }.inStock)
      org.junit.Assert.assertTrue(repo3.observeAnnouncements().first().first { it.id == firstAnnouncement.id }.isActive)

      context.getSharedPreferences("elegan_publication_states", Context.MODE_PRIVATE).edit().clear().commit()
    }
  }

  @Test
  fun `test support chat sends and persists image and video messages`() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      context.getSharedPreferences("elegan_support_messages", Context.MODE_PRIVATE).edit().clear().commit()
      com.example.data.remote.MobileSession.clear(context)
      com.example.data.remote.MobileSession.loginClientLocally(context)

      val repo1 = com.example.data.repository.EleganRepository(context)
      repo1.sendSupportMessage(
        text = "Adjunto foto del producto",
        mediaUri = "https://example.com/foto.jpg",
        mediaType = "image",
        scope = this
      )
      repo1.sendSupportMessage(
        text = "",
        mediaUri = "https://example.com/video.mp4",
        mediaType = "video",
        scope = this
      )

      val msgs1 = repo1.observeMessages().first()
      val imgMsg = msgs1.first { it.mediaType == "image" }
      val vidMsg = msgs1.first { it.mediaType == "video" }
      assertEquals("user", imgMsg.sender)
      assertEquals("https://example.com/foto.jpg", imgMsg.mediaUrl)
      assertEquals("Adjunto foto del producto", imgMsg.text)
      assertEquals("user", vidMsg.sender)
      assertEquals("https://example.com/video.mp4", vidMsg.mediaUrl)

      // Simular recarga con una nueva instancia del repositorio
      val repo2 = com.example.data.repository.EleganRepository(context)
      val msgs2 = repo2.observeMessages().first()
      org.junit.Assert.assertTrue(msgs2.any { it.id == imgMsg.id && it.mediaType == "image" })
      org.junit.Assert.assertTrue(msgs2.any { it.id == vidMsg.id && it.mediaType == "video" })

      context.getSharedPreferences("elegan_support_messages", Context.MODE_PRIVATE).edit().clear().commit()
      com.example.data.remote.MobileSession.clear(context)
    }
  }
}
