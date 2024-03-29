package com.example.menagerie

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SearchPageFragment(
    private val search: ItemSearch,
    private val page: Int,
    private val onItemClick: ((item: Item, position: Int) -> Unit)? = null
) : Fragment() {

    private lateinit var errorText: TextView
    private lateinit var errorIcon: ImageView
    private lateinit var recycler: RecyclerView
    private lateinit var swipeRefresher: SwipeRefreshLayout
    private lateinit var recyclerAdapter: ThumbnailAdapter

    private val handler: Handler = Handler(Looper.getMainLooper())

    var items: MutableLiveData<MutableList<Item>> = MutableLiveData() // TODO extract this out into viewmodel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_page, container, false)

        errorText = view.findViewById(R.id.pageErrorText)
        errorIcon = view.findViewById(R.id.pageErrorIcon)
        recycler = view.findViewById(R.id.pageRecycler)
        swipeRefresher = view.findViewById(R.id.pageSwipeRefresh)

        swipeRefresher.setOnRefreshListener {
            requestPage()
        }
        initializeRecyclerView()
        initializeToTopButton(view)

        return view
    }

    override fun onStart() {
        super.onStart()

        val cached = search.pageCache[page]
        if (cached == null) {
            requestPage()
        } else {
            items.postValue(ArrayList(cached))
        }
    }

    private fun initializeRecyclerView() {
        recycler.onGlobalLayout {
            val span = recycler.width / dpToPixels(resources, PREFERRED_THUMBNAIL_SIZE_DP)
            recyclerAdapter = ThumbnailAdapter(span, onItemClick)

            if (items.value != null) {
                recyclerAdapter.pageData = items.value
                recyclerAdapter.notifyDataSetChanged()
            }

            recycler.apply {
                layoutManager = GridLayoutManager(context, span)
                adapter = recyclerAdapter
            }
        }

        items.observe(viewLifecycleOwner) {
            if (recycler.adapter != null) {
                handler.post {
                    (recycler.adapter as ThumbnailAdapter).apply {
                        pageData = it
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun initializeToTopButton(view: View) {
        val toTopButton = view.findViewById<FloatingActionButton>(R.id.pageToTopButton)
        toTopButton.setOnClickListener { toTopOfPage(toTopButton) }
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                toTopButton.visibility =
                    if ((recyclerView.layoutManager as GridLayoutManager).findFirstVisibleItemPosition() > 0) View.VISIBLE else View.GONE
            }
        })
    }

    private fun toTopOfPage(@Suppress("UNUSED_PARAMETER") view: View) {
        if ((recycler.layoutManager as GridLayoutManager).findLastVisibleItemPosition() < 100)
            recycler.smoothScrollToPosition(0)
        else
            recycler.scrollToPosition(0)
    }

    private fun updateStatus(
        progress: Boolean = false,
        errorMessage: String? = null
    ) {
        activity?.runOnUiThread {
            swipeRefresher.isRefreshing = progress

            if (errorMessage != null) {
                errorText.visibility = View.VISIBLE
                errorIcon.visibility = View.VISIBLE
                errorText.text = errorMessage
            } else {
                errorText.visibility = View.GONE
                errorIcon.visibility = View.GONE
            }
        }
    }

    private fun requestPage() {
        updateStatus(progress = true)

        search.request(page, success = { _, items ->
            updateStatus()

            this.items.postValue(ArrayList(items))
        }, failure = { _, e ->
            updateStatus(errorMessage = "Failed to connect\n${APIClient.address}")
            e?.printStackTrace()
        })
    }

}