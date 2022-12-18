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
import com.gallery.example.databinding.FCropEditBinding
import com.google.android.material.snackbar.Snackbar
import com.hmju.permissions.extension.dp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Description :
 *
 * Created by juhongmin on 2022/12/12
 */
@AndroidEntryPoint
internal class CropImageEditFragment : Fragment() {

    private val viewModel: CropImageEditFragmentViewModel by viewModels()

    private lateinit var binding: FCropEditBinding

    private val cameraCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.onSuccessSavePicture()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DataBindingUtil.inflate<FCropEditBinding>(
            inflater,
            R.layout.f_crop_edit,
            container, false
        ).run {
            binding = this
            lifecycleOwner = this@CropImageEditFragment
            setVariable(BR.vm, viewModel)
            this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            rvGallery.setRequestManager(Glide.with(this@CropImageEditFragment))
            rvGallery.addItemDecoration(GridDividerItemDecoration(2.dp))
            rvGallery.setLifecycle(this@CropImageEditFragment)
        }

        with(viewModel) {

            startViewHolderClickEvent.observe(viewLifecycleOwner) {
                binding.rvGallery.requestViewHolderClick(it)
            }

            startSnackBarEvent.observe(viewLifecycleOwner) {
                Snackbar.make(view, it, Snackbar.LENGTH_SHORT).show()
            }

            startCameraOpenEvent.observe(viewLifecycleOwner) {
                cameraCallback.launch(it)
            }

            start()
        }
    }
}
