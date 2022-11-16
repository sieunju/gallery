package com.gallery.example.edit

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.gallery.core.GalleryProvider
import com.gallery.example.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
internal class EditFragment : Fragment(R.layout.fragment_edit) {

    @Inject
    lateinit var galleryProvider: GalleryProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            findViewById<AppCompatTextView>(R.id.bFlexible).setOnClickListener {
                moveToFragment(FlexibleEditFragment())
            }

            findViewById<AppCompatTextView>(R.id.bCropEdit).setOnClickListener {
                moveToFragment(CropEditFragment())
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
