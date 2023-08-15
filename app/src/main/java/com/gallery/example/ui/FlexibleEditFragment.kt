package com.gallery.example.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.gallery.example.BR
import com.gallery.example.R
import com.gallery.example.databinding.FFlexibleEditBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

/**
 * Description : Gallery FlexibleImageEditView Example Fragment
 *
 * Created by juhongmin on 2022/12/12
 */
@AndroidEntryPoint
internal class FlexibleEditFragment : Fragment() {

    private val viewModel: FlexibleEditFragmentViewModel by viewModels()

    private lateinit var binding: FFlexibleEditBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DataBindingUtil.inflate<FFlexibleEditBinding>(
            inflater,
            R.layout.f_flexible_edit,
            container, false
        ).run {
            binding = this
            lifecycleOwner = this@FlexibleEditFragment
            setVariable(BR.vm, viewModel)
            this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            rvGallery.setRequestManager(Glide.with(this@FlexibleEditFragment))
            rvGallery.addItemDecoration(GridDividerItemDecoration(25))
            rvGallery.setLifecycle(this@FlexibleEditFragment)
            cvCrop.setOnClickListener {
                if (it.isSelected) {
                    edit.centerCrop()
                    it.isSelected = false
                } else {
                    edit.fitCenter()
                    it.isSelected = true
                }
            }
        }

        with(viewModel) {
            startViewHolderClickEvent.observe(viewLifecycleOwner) {
                binding.rvGallery.requestViewHolderClick(it)
            }

            startSnackBarEvent.observe(viewLifecycleOwner) {
                Snackbar.make(view, it, Snackbar.LENGTH_SHORT).show()
            }

            startSendEditImageBitmap.observe(viewLifecycleOwner) {
                showCaptureDialog(it)
            }

            start()
        }
    }

    private fun showCaptureDialog(list: List<Bitmap>) {
        CaptureBottomSheetFragment()
            .setBitmapList(list)
            .simpleShow(childFragmentManager)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearDisposable()
    }
}