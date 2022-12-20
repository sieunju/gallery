package com.gallery.example

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.gallery.example.ui.CropImageEditFragment
import com.gallery.example.ui.FlexibleEditFragment
import com.gallery.example.ui.GalleryBottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hmju.permissions.SimplePermissions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val extendFab: FloatingActionButton by lazy { findViewById(R.id.extendFab) }

    private val fabFlexible: FloatingActionButton by lazy { findViewById(R.id.fabFlexible) }
    private val tvFlexible: TextView by lazy { findViewById(R.id.tvFlexible) }

    private val fabCropEdit: FloatingActionButton by lazy { findViewById(R.id.fabCropEdit) }
    private val tvCropEdit: TextView by lazy { findViewById(R.id.tvCropEdit) }

    private val fabBottomSheet: FloatingActionButton by lazy { findViewById(R.id.fabBottomSheet) }
    private val tvBottomSheet: TextView by lazy { findViewById(R.id.tvBottomSheet) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        extendFab.setOnClickListener {
            performExtendFabButton()
        }

        fabFlexible.setOnClickListener {
            moveToFragment(FlexibleEditFragment())
            performFabButton(false)
        }

        fabCropEdit.setOnClickListener {
            moveToFragment(CropImageEditFragment())
            performFabButton(false)
        }

        fabBottomSheet.setOnClickListener {
            GalleryBottomSheetDialog()
                .simpleShow(supportFragmentManager)
            performFabButton(false)
        }

        SimplePermissions(this)
            .requestPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .build { b, strings -> }

        // moveToFragment(FlexibleEditFragment())
    }

    private fun performExtendFabButton() {
        if (extendFab.isSelected) {
            performFabButton(false)
            extendFab.isSelected = false
        } else {
            performFabButton(true)
            extendFab.isSelected = true
        }
    }

    private fun performFabButton(isShow: Boolean) {
        if (isShow) {
            fabFlexible.show()
            fabCropEdit.show()
            fabBottomSheet.show()
            tvFlexible.visibility = View.VISIBLE
            tvCropEdit.visibility = View.VISIBLE
            tvBottomSheet.visibility = View.VISIBLE
        } else {
            fabFlexible.hide()
            fabCropEdit.hide()
            fabBottomSheet.hide()
            tvFlexible.visibility = View.GONE
            tvCropEdit.visibility = View.GONE
            tvBottomSheet.visibility = View.GONE
        }
    }

    private fun moveToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.container, fragment)
            addToBackStack(null)
            commit()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            finishAffinity()
        }
    }
}