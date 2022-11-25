package com.gallery.example.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.gallery.example.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Description : Gallery Root Fragment
 *
 * Created by juhongmin on 2022/11/25
 */
@AndroidEntryPoint
internal class GalleryRootFragment : Fragment(R.layout.fragment_gallery) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            findViewById<AppCompatTextView>(R.id.bAddCamera).setOnClickListener {
                moveToFragment(AddCameraGalleryFragment())
            }

            findViewById<AppCompatTextView>(R.id.bNoCamera).setOnClickListener {
                moveToFragment(NoCameraGalleryFragment())
            }
        }
    }

    private fun moveToFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction().apply {
            replace(R.id.subFragment, fragment)
            addToBackStack(null)
            commit()
        }
    }
}

