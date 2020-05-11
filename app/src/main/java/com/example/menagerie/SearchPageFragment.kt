package com.example.menagerie

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SearchPageFragment(private val model: SearchViewModel) : Fragment() {

    private lateinit var errorText: TextView
    private lateinit var errorIcon: ImageView
    private lateinit var recycler: RecyclerView
    private lateinit var swipeRefresher: SwipeRefreshLayout
    private lateinit var recyclerAdapter: ThumbnailAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_page, container, false)

        errorText = view.findViewById(R.id.gridErrorText)
        errorIcon = view.findViewById(R.id.gridErrorIcon)
        recycler = view.findViewById(R.id.grid)
        swipeRefresher = view.findViewById(R.id.searchSwipeRefresh)

        swipeRefresher.setOnRefreshListener {
            // TODO refresh the page contents
        }
        initializeRecyclerView()
        initializeToTopButton(view)

        showStatus()

        return view
    }

    private fun initializeRecyclerView() {
        recycler.onGlobalLayout {
            val span = recycler.width / dpToPixels(resources, PREFERRED_THUMBNAIL_SIZE_DP)
            recyclerAdapter = ThumbnailAdapter(span)

            recycler.apply {
                layoutManager = GridLayoutManager(context, span)
                adapter = recyclerAdapter
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

    private fun showStatus(
        progress: Boolean = false,
        error: Boolean = false,
        errorMessage: String? = null
    ) {
        swipeRefresher.isRefreshing = progress

        errorText.visibility = if (error) View.VISIBLE else View.GONE
        errorIcon.visibility = if (error) View.VISIBLE else View.GONE
        if (error && errorMessage != null) errorText.text = errorMessage
    }

}