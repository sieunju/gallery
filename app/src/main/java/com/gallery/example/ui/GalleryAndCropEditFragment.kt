package com.gallery.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.gallery.core.GalleryProvider
import com.gallery.example.BR
import com.gallery.example.R
import com.gallery.example.databinding.FGalleryAndCropEditBinding
import com.gallery.example.databinding.FGalleryAndFlexibleBinding
import com.gallery.ui.GalleryRecyclerView
import com.hmju.permissions.extension.dp
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject

@AndroidEntryPoint
internal class GalleryAndCropEditFragment : Fragment() {

    private val viewModel: GalleryAndCropEditFragmentViewModel by viewModels()

    private lateinit var binding: FGalleryAndCropEditBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return DataBindingUtil.inflate<FGalleryAndCropEditBinding>(
            inflater,
            R.layout.f_gallery_and_crop_edit,
            container,
            false
        ).run {
            binding = this
            lifecycleOwner = this@GalleryAndCropEditFragment
            setVariable(BR.vm, viewModel)
            this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            rvGallery.setRequestManager(Glide.with(this@GalleryAndCropEditFragment))
            rvGallery.addItemDecoration(GridDividerItemDecoration(2.dp))
            rvGallery.setLifecycle(this@GalleryAndCropEditFragment)
        }

        with(viewModel) {
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearDisposable()
    }
}
