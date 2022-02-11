package chingyu.poearchnemesismods

import android.app.SearchManager
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.database.getStringOrNull
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.databinding.DataBindingUtil
import chingyu.poearchnemesismods.databinding.ActivityMainBinding
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity(), OnDataPass {

    private lateinit var binding: ActivityMainBinding
    private val mods = ArchnemesisMod.values().toList()

    // avoiding ambiguity namespace ex: adapter: MyExpandableListAdapter
    private lateinit var expAdapter: MyExpandableListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // listview
        handleListView()

        // appbar
        handleTopAppbar()

    }

    private fun handleTopAppbar() {
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.filter -> {
                    showDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun handleListView() {
        expAdapter = MyExpandableListAdapter(mods)
        val header = layoutInflater.inflate(R.layout.header_layout, null)
        binding.expListView.apply {
            setAdapter(expAdapter)
            addHeaderView(header)
        }

        binding.expListView.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->

            val mod = expAdapter.getClickedMod(groupPosition, childPosition)
            var position = expAdapter.getModPosition(mod.modName)

            // when clicked item is not on list
            if (position == null) {
                // reset data
                expAdapter.updateData(mods)
                // re map<name,position>
                binding.expListView.smoothScrollToPositionFromTop(parent.count, 0, 200)
                Timer().schedule(500) {
                    do {
                        position = expAdapter.getModPosition(mod.modName)
                    } while (position == null)
                    runOnUiThread {
                        binding.expListView.setSelectedGroup(position!!)
                        // make sure that listview move to correct position
                        binding.expListView.setSelectedGroup(position!!)
                    }
                }
            } else {
                binding.expListView.setSelectedGroup(position!!)
            }

            false // *** ture if the click is handle
        }
    }

    // searchview
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        val searchView = menu?.findItem(R.id.search)?.actionView as? SearchView ?: return false
        // hide search icon
        searchView.findViewById<AutoCompleteTextView>(R.id.search_src_text).threshold = 1

        // This is the line of text and icon that will be presented to the user as the suggestion
        val from = arrayOf("SUGGESTION_ICON", SearchManager.SUGGEST_COLUMN_TEXT_1)
        val to = intArrayOf(R.id.suggestion_icon, R.id.suggestion_text)
        val cursorAdapter = SimpleCursorAdapter(
            this, R.layout.suggestion_layout, null, from, to,
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )
        val suggestions: MutableList<Pair<Int, String>> =
            emptyList<Pair<Int, String>>().toMutableList()
        mods.forEach {
            suggestions.add(Pair(it.icon, it.modName))
        }

        cursorAdapter.setViewBinder { view, cursor, columnIndex ->
            val iconIndex = cursor.getColumnIndex("SUGGESTION_ICON")
            val textIndex = cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
            if (iconIndex == columnIndex) {
                val resId = cursor.getInt(iconIndex)
                (view as ImageView).setImageResource(resId)
            }
            if (textIndex == columnIndex) {
                val resId = cursor.getString(textIndex)
                (view as TextView).text = resId
            }
            true
        }

        searchView.suggestionsAdapter = cursorAdapter
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query.isNullOrEmpty()) {
                    expAdapter.updateData(mods)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val cursor = MatrixCursor(
                    arrayOf(
                        BaseColumns._ID,
                        "SUGGESTION_ICON",
                        SearchManager.SUGGEST_COLUMN_TEXT_1
                    )
                )
                newText?.let {

                    suggestions.forEachIndexed { index, suggestion ->
                        if (suggestion.second.contains(it, true)) {
                            cursor.addRow(arrayOf(index, suggestion.first, suggestion.second))
                        }
                    }
                    val result = searchGroupsAndChild(it)
                    expAdapter.updateData(result)
                }
                cursorAdapter.changeCursor(cursor)
                return true
            }
        })

        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                hideKeyboard(searchView)
                val cursor = searchView.suggestionsAdapter.getItem(position) as Cursor
                val selection =
                    cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1))
                searchView.setQuery(selection, false)

                return true
            }
        })
        
        val searchActionMenu = menu.findItem(R.id.search)
        val filterActionMenu = menu.findItem(R.id.filter)

        if(searchActionMenu is MenuItem){
            searchActionMenu.setOnActionExpandListener(object : MenuItem.OnActionExpandListener{
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    filterActionMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    invalidateOptionsMenu()
                    return true
                }

            })
        }

        return true
    }

    private fun searchGroupsAndChild(str: String): List<ArchnemesisMod> {
        val result: MutableSet<ArchnemesisMod> = emptySet<ArchnemesisMod>().toMutableSet()
        mods.forEach {
            // search through group
            if (it.modName.contains(str, true)) {
                result.add(it)
            }
            // search through recipe
            it.recipe?.forEach { child ->
                if (child.modName.contains(str, true)) {
                    result.add(it)
                }
            }
        }
        return result.toList()
    }

    private fun showDialog() {
        val fragmentManager = supportFragmentManager
        val newFragment = RewardFilterDialog()
        newFragment.show(fragmentManager, "dialog")
    }

    override fun onDataPass(tag: Int) {
        filterByReward(tag)
    }

    private fun filterByReward(tag: Int) {
        if (tag == 0) {
            expAdapter.updateData(mods)
            return
        }
        val set: MutableSet<ArchnemesisMod> = emptySet<ArchnemesisMod>().toMutableSet()
        mods.forEach { mod ->
            if (mod.rewards != null)
                mod.rewards.forEach {
                    if (it == tag) {
                        set.add(mod)
                    }
                }
        }
        expAdapter.updateData(set.toList())
        // move to top
        binding.expListView.setSelectedGroup(0)
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }


}