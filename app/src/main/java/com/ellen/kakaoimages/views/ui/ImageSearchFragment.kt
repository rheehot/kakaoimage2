package com.ellen.kakaoimages.views.ui

import com.ellen.kakaoimages.R
import com.ellen.kakaoimages.viewmodel.ImageViewModel
import com.ellen.kakaoimages.views.adapter.ImageListAdapter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.ellen.kakaoimages.databinding.FragmentSearchImageBinding
import com.ellen.kakaoimages.util.Constants.Companion.FILTER
import com.ellen.kakaoimages.util.EndlessRecyclerViewScrollListener
import com.ellen.kakaoimages.util.hideKeyboard
import kotlinx.android.synthetic.main.fragment_search_image.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.viewmodel.ext.android.sharedViewModel

class ImageSearchFragment : Fragment() {

    private val vm: ImageViewModel by sharedViewModel()
    private lateinit var imageListAdapter: ImageListAdapter
    private lateinit var mViewDataBinding: FragmentSearchImageBinding
    private lateinit var scrollListener: EndlessRecyclerViewScrollListener
   private var job: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mViewDataBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_search_image, container, false
        )
        val mRootView = mViewDataBinding.root
        mViewDataBinding.lifecycleOwner = this
        return mRootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        /**
         * RecyclerView
         */
        setUpRecyclerView()
        /**
         * EditText
         */
        setupEditText(ed_search)

        mViewDataBinding.viewModel = vm
        vm.userList.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty() && it != null) {
                imageListAdapter.setImages(it)
            }
        })

    }

    private fun initPage() {
        vm.init()
        imageListAdapter.clear()
        scrollListener.resetState()
    }

    private fun setUpRecyclerView() {
        val linearLayoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
        scrollListener = object : EndlessRecyclerViewScrollListener(linearLayoutManager) {
            var isKeyboardDismissedByScroll = false

            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                vm.fetchImages(page)
            }

            //Hide Keyboard when scroll Dragging
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (!isKeyboardDismissedByScroll) {
                        hideKeyboard()
                        isKeyboardDismissedByScroll = !isKeyboardDismissedByScroll
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isKeyboardDismissedByScroll = false
                }
            }
        }

        imageListAdapter = ImageListAdapter()
        rv_search_user.apply {
            layoutManager = linearLayoutManager
            addOnScrollListener(scrollListener)
            rv_search_user.adapter = imageListAdapter
        }

        imageListAdapter.setOnItemClickListener {
            vm.select(it)
//            vm.showDetailFragment()
            FILTER = it.collection
            imageListAdapter.filter.filter(FILTER)
        }
    }

    private fun setupEditText(ed: EditText) {

        ed.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                val clearIcon = if (editable?.isNotEmpty() == true) R.drawable.ic_clear else 0
                ed.setCompoundDrawablesWithIntrinsicBounds(0, 0, clearIcon, 0)
                job?.cancel()
                job = MainScope().launch {
                    delay(500L)
                    editable?.let {
                        if (editable.toString().isNotEmpty()) {
                            //init
                            initPage()
                            vm.fetchImages(1)
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =Unit
        })

        ed.setOnTouchListener(View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= ((v as EditText).right - v.compoundPaddingRight)) {
                    v.setText("")
                    initPage()
                    return@OnTouchListener true
                }
            }
            return@OnTouchListener false
        })
    }
}
