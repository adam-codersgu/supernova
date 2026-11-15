package com.codersguidebook.supernova.ui.album

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.testutils.ReflectionUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.Robolectric
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

    /**
     * fixme
     * Long-term improvement for BaseFragment
     * If BaseFragment only calls a specific helper or interface method on MainActivity, extracting an interface (e.g., MusicHost) implemented by MainActivity lets BaseFragment check activity as? MusicHost instead of hard-casting to MainActivity, which makes isolated launchFragmentInContainer tests work out of the box.
     */
    @Suppress("UNCHECKED_CAST")
    @Nested
    inner class OnCreateView {

        @Test
        fun onCreateView() {
            /* val controller = Robolectric.buildActivity(MainActivity::class.java).create()
            val activity = controller.get()

            val fragment = EditAlbumFragment().apply {
                arguments = bundleOf("albumId" to "album-123")
            }

            activity.supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, fragment, "edit_album_tag")
                .commitNow()

            assertNotNull(fragment.view)
            assertNotNull(fragment._binding)
            assertNotNull(fragment.binding)
            val albumIdField = ReflectionUtils.setFieldVisible(fragment, "albumId") as KMutableProperty1<Any, String>
            assertEquals("album-123", albumIdField.get(fragment)) */

            /* val controller = Robolectric.buildActivity(MainActivity::class.java).create()
            val hostActivity = controller.get()
            val container = FrameLayout(hostActivity)

            val fragmentArgs = Bundle()
            fragmentArgs.putString("albumId", "album-123")

            val fragment = EditAlbumFragment().apply {
                arguments = fragmentArgs
            }

            hostActivity.supportFragmentManager
                .beginTransaction()
                .add(fragment, "edit_album_tag")
                .commitNowAllowingStateLoss()

            val inflater = LayoutInflater.from(ApplicationProvider.getApplicationContext())
            val view = fragment.onCreateView(inflater, container, Bundle.EMPTY)

            assertNotNull(view)
            assertNotNull(fragment._binding)
            val albumIdField = ReflectionUtils.setFieldVisible(fragment, "albumId") as KMutableProperty1<Any, String>
            assertEquals("album-123", albumIdField.get(fragment)) */

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