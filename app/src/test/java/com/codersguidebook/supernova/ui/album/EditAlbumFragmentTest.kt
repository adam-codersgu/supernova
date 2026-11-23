package com.codersguidebook.supernova.ui.album

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import com.codersguidebook.supernova.testutils.ReflectionUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension
import kotlin.reflect.KMutableProperty1

@ExtendWith(RobolectricExtension::class)
class EditAlbumFragmentTest {

    /**
     * TODO
     * RESUME
     *  START ABSTRACTING MAINACTIVITY METHODS USED BY FRAGMENTS INTO AN INTERFACE AND USE THAT IN BASEFRAGMENT
     *  E.G. activity as? MusicHost
     *  Don’t touch viewLifecycleOwner inside onCreateView:
     *  Move setupMenu() into onViewCreated(view, savedInstanceState) (which is the standard Fragment lifecycle hook for view/menu setup once view is guaranteed non-null), or pass a MenuHost / helper in so onCreateView only inflates the binding and reads arguments.
     *
     */
    @Suppress("UNCHECKED_CAST")
    @Nested
    inner class OnCreateView {

        @Test
        fun onCreateView() {
            val fragmentArgs = Bundle()
            fragmentArgs.putString("albumId", "album-123")

            val scenario = launchFragmentInContainer<EditAlbumFragment>(
                fragmentArgs = fragmentArgs,
            )

            scenario.onFragment { fragment ->
                assertNotNull(fragment._binding)
                assertNotNull(fragment.binding)
                val albumIdField = ReflectionUtils.setFieldVisible(fragment, "albumId") as KMutableProperty1<Any, String>
                assertEquals("album-123", albumIdField.get(fragment))
            }

        }
    }
}