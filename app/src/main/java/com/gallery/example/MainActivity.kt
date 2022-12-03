package com.gallery.example

import android.Manifest
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.gallery.example.core.CoreFragment
import com.gallery.example.edit.EditFragment
import com.gallery.example.ui.GalleryRootFragment
import com.hmju.permissions.SimplePermissions
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.bCore).setOnClickListener {
            moveToFragment(CoreFragment())
        }

        findViewById<Button>(R.id.bEdit).setOnClickListener {
            moveToFragment(EditFragment())
        }

        findViewById<Button>(R.id.bUi).setOnClickListener {
            moveToFragment(GalleryRootFragment())
        }

        moveToFragment(CoreFragment())

        SimplePermissions(this)
            .requestPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .build { b, strings -> }
    }

    private fun moveToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.container, fragment)
            addToBackStack(null)
            commit()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            finishAffinity()
        }
    }
}