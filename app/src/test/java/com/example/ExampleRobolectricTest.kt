package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
}
