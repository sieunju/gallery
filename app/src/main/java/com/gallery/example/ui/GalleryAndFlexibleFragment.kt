package com.gallery.example.ui

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.gallery.example.BR
import com.gallery.example.R
import com.gallery.example.databinding.FGalleryAndFlexibleBinding
import com.google.android.material.snackbar.Snackbar
import com.hmju.permissions.extension.dp
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
internal class GalleryAndFlexibleFragment : Fragment() {

    private val viewModel: GalleryAndFlexibleFragmentViewModel by viewModels()

    private lateinit var binding: FGalleryAndFlexibleBinding

    private val cameraCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.onSuccessSavePicture()
            } else {
                Timber.d("CameraCallback Fail")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DataBindingUtil.inflate<FGalleryAndFlexibleBinding>(
            inflater,
            R.layout.f_gallery_and_flexible,
            container,
            false
        ).run {
            binding = this
            lifecycleOwner = this@GalleryAndFlexibleFragment
            setVariable(BR.vm, viewModel)
            this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            rvGallery.setRequestManager(Glide.with(this@GalleryAndFlexibleFragment))
            rvGallery.addItemDecoration(GridDividerItemDecoration(2.dp))
            rvGallery.setLifecycle(this@GalleryAndFlexibleFragment)
        }

        with(viewModel) {

            startViewHolderClickEvent.observe(viewLifecycleOwner) {
                binding.rvGallery.requestViewHolderClick(it)
            }

            startSaveStateItem.observe(viewLifecycleOwner) {
                setCurrentStateItem(it, binding.ivFlexible.getFlexibleStateItem())
            }

            startCameraOpenEvent.observe(viewLifecycleOwner) {
                cameraCallback.launch(it)
            }

            startSnackBarEvent.observe(viewLifecycleOwner) {
                Snackbar.make(view, it, Snackbar.LENGTH_SHORT).show()
            }

            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearDisposable()
    }
}
