package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.catalog.GameCatalog
import com.example.data.model.CharacterId
import com.example.data.model.CompanionId
import com.example.data.model.ConsumableType
import com.example.data.model.EnemyType
import com.example.ui.components.PixelArtSprites
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals("Toybox Nightmare", appName)
  }

  @Test
  fun `all pixel art sprites and object icons render with valid dimensions`() {
    assertEquals(27, EnemyType.entries.size)
    EnemyType.entries.forEach { enemy ->
      val bmp = PixelArtSprites.forEnemy(enemy, disguised = false)
      assertTrue("${enemy.name} invalid size", bmp.width >= 12 && bmp.height >= 10)
    }
    assertTrue(PixelArtSprites.forEnemy(EnemyType.CREEPY_DOLL, disguised = true).width >= 12)
    assertTrue(PixelArtSprites.forEnemy(EnemyType.MIMIC_CHEST, disguised = true).width >= 12)
    assertTrue(PixelArtSprites.forEnemy(EnemyType.WEEPING_TOY, disguised = true).width >= 12)

    assertEquals(8, CompanionId.entries.size)
    CompanionId.entries.forEach { comp ->
      val bmp = PixelArtSprites.forCompanion(comp, broken = false)
      assertTrue("${comp.name} invalid size", bmp.width >= 12 && bmp.height >= 10)
      val brokenBmp = PixelArtSprites.forCompanion(comp, broken = true)
      assertTrue("${comp.name} broken invalid size", brokenBmp.width >= 12 && brokenBmp.height >= 10)
    }

    CharacterId.entries.forEach { cid ->
      val bmp = PixelArtSprites.forCharacter(cid)
      assertTrue("${cid.name} invalid size", bmp.width >= 12 && bmp.height >= 10)
    }

    assertEquals(20, GameCatalog.passiveItems.size)
    GameCatalog.passiveItems.forEach { item ->
      val bmp = PixelArtSprites.forPassiveItem(item.id)
      assertTrue("${item.id} invalid size", bmp.width >= 10 && bmp.height >= 8)
    }

    assertEquals(8, ConsumableType.entries.size)
    ConsumableType.entries.forEach { c ->
      val bmp = PixelArtSprites.forConsumable(c)
      assertTrue("${c.name} invalid size", bmp.width >= 10 && bmp.height >= 7)
    }

    GameCatalog.bodyParts.forEach { part ->
      val bmp = PixelArtSprites.forBodyPart(part.id, part.zone)
      assertTrue("${part.id} invalid size", bmp.width >= 10 && bmp.height >= 5)
    }

    assertTrue(PixelArtSprites.vitrineObstacle().width >= 10)
    assertTrue(PixelArtSprites.pillarObstacle().width >= 10)
    assertTrue(PixelArtSprites.plushPileObstacle().width >= 10)
    assertTrue(PixelArtSprites.sewingTableProp().width >= 16)
    assertTrue(PixelArtSprites.threadSpoolsProp().width >= 12)
    assertTrue(PixelArtSprites.mannequinProp().width >= 10)
    assertTrue(PixelArtSprites.scissorsProp().width >= 12)
    assertTrue(PixelArtSprites.nurseryBlockObstacle().width >= 12)
    assertTrue(PixelArtSprites.companionBellProp().width >= 10)
  }
}
