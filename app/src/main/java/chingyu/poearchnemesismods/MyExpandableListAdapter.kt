package chingyu.poearchnemesismods

import android.database.DataSetObserver
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import chingyu.poearchnemesismods.databinding.ChildLayoutBinding
import chingyu.poearchnemesismods.databinding.ModLayoutBinding

class MyExpandableListAdapter(private var mods: List<ArchnemesisMod>): BaseExpandableListAdapter() {

    private var map = mutableMapOf<String,Int>()
    private val idList: MutableList<Int> = emptyList<Int>().toMutableList()

    init{
        // id list for image view
        for (i in 0..7){
            idList.add(generateViewId())
        }
    }

    fun updateData(data: List<ArchnemesisMod>){
        mods = data
        notifyDataSetChanged()
        map.clear()
    }

    // This function is used to get the total number of groups.
    override fun getGroupCount(): Int {
        return mods.size
    }

    // This function gets the number of children in a specified group.
    override fun getChildrenCount(groupPosition: Int): Int {
        // return 0 if left hand side is null
        return mods[groupPosition].recipe?.size ?: 0
    }

    // This method gets the data associated with the given group.
    override fun getGroup(groupPosition: Int): Any {
        return mods[groupPosition]
    }

    // This method gets the data associated with the given child within the given group.
    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return mods[groupPosition].recipe?.get(childPosition) as Any
    }

    // This function is used to get the ID for the group at the given position.
    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    // This function is used to gets the ID for the given child within the given group.
    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return (groupPosition * 100 + childPosition).toLong()
    }

    // This method Indicates that whether the child and group ID’s are
    // stable across changes to the underlying data.
    override fun hasStableIds(): Boolean {
        // return whether or not the same ID always refers to the same object
        return false
    }

    // This method is used when we need to create our group or parent View
    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val mod = mods[groupPosition]

        val binding: ModLayoutBinding = if(convertView == null){
            val layoutInflater = LayoutInflater.from(parent?.context)
            DataBindingUtil.inflate(layoutInflater, R.layout.mod_layout, parent, false)
        }else{
            DataBindingUtil.bind(convertView)!!
        }

        setLayoutBackgroundColor(groupPosition,binding.modLayout)

        binding.apply {
            modIcon.setImageResource(mod.icon)
            modName.text = mod.modName
            if(mod.description != 0){
                modDesc.setText(mod.description)
            }else{
                modDesc.text = ""
            }
        }

//         map<modName,position>
        map[mod.modName] = groupPosition
//        Log.i("Map name and position","name: ${mod.modName} p: $groupPosition")

        // set custom indicator icon
        if(getChildrenCount( groupPosition ) == 0 ) {
            binding.indicator.visibility = View.INVISIBLE
        }else{
            binding.indicator.visibility = View.VISIBLE
            val result = if(isExpanded) R.drawable.ic_baseline_arrow_drop_up_24 else R.drawable.ic_baseline_arrow_drop_down_24
            binding.indicator.setImageResource(result)
        }

        if(mods[groupPosition].rewards?.isNotEmpty() == true){
            setRewardIcon(groupPosition, binding)
        }else{
            // clear view
            binding.apply {
                reward1.setImageResource(android.R.color.transparent)
                reward2.setImageResource(android.R.color.transparent)
                reward3.setImageResource(android.R.color.transparent)
                reward4.setImageResource(android.R.color.transparent)
            }
        }

        return binding.root
    }

    private fun setRewardIcon(
        groupPosition: Int,
        binding: ModLayoutBinding
    ) {
        val resList: MutableList<Int> = emptyList<Int>().toMutableList()

        mods[groupPosition].rewards!!.forEach { res ->
            resList.add(res)
        }

        // fill rest of the list with empty
        if (resList.size < 4) {
            for (i in resList.size..4) {
                resList.add(android.R.color.transparent)
            }
        }

        binding.apply {
            reward1.setImageResource(resList[0])
            reward2.setImageResource(resList[1])
            reward3.setImageResource(resList[2])
            reward4.setImageResource(resList[3])
        }
    }


    // generate imageView via code
    private fun setRewardIcon(
        groupPosition: Int,
        binding: ModLayoutBinding,
        parent: ViewGroup?
    ) {
        val rewardList = mods[groupPosition].rewards
        val set = ConstraintSet()
        set.clone(binding.modLayout)
        val imageView = ImageView(parent?.context)

        // clear image
//        idList.forEach { generatedId ->
//            imageView.id = generatedId
//            imageView.setImageResource(android.R.color.transparent)
//            imageView.setImageDrawable(null)
//            imageView.visibility = View.GONE
//        }

        // set image
        var index = 0
        rewardList!!.forEach {

            imageView.id = idList[index]
            if (imageView.parent != null) {
                (imageView.parent as ViewGroup).removeView(imageView)
            }
            imageView.setImageResource(it)
            binding.modLayout.addView(imageView)

            // layout_constraintTop_toBottomOf="@+id/mod_name"
            set.connect(
                idList[index], ConstraintSet.TOP,
                binding.modName.id, ConstraintSet.BOTTOM,
                8
            )
            // layout_constraintBottom_toTopOf="@+id/mod_desc"
            set.connect(idList[index], ConstraintSet.BOTTOM,
                binding.modDesc.id, ConstraintSet.TOP,
                8
            )

            if (index == 0) {
                // layout_constraintStart_toStartOf="@+id/mod_name"
                set.connect(idList[index], ConstraintSet.START,
                    binding.modName.id, ConstraintSet.START,
                    8
                )
            } else {
                // layout_constraintStart_toEndOf="reward_icon"
                set.connect(idList[index], ConstraintSet.START,
                    idList[index-1], ConstraintSet.END,
                    8)
            }
            set.constrainHeight(idList[index], LayoutParams.WRAP_CONTENT)
            index += 1
            imageView.visibility = View.VISIBLE
            set.applyTo(binding.modLayout)
        }
    }

    // This method is  used when we need to create a child View means
    // a child item for a parent or group.
    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {

//        val layoutInflater = LayoutInflater.from(parent?.context)
//        val binding: ChildLayoutBinding =
//            DataBindingUtil.inflate(layoutInflater, R.layout.child_layout, parent, false)

        val binding: ChildLayoutBinding = if(convertView == null){
            val layoutInflater = LayoutInflater.from(parent?.context)
            DataBindingUtil.inflate(layoutInflater, R.layout.child_layout, parent, false)
        }else{
            DataBindingUtil.bind(convertView)!!
        }

        setLayoutBackgroundColor(groupPosition,binding.childLayout)

        // set child view icon and text
        val imageResource = mods[groupPosition].recipe!![childPosition].icon
        val text = mods[groupPosition].recipe!![childPosition].modName
        binding.apply {
            childIcon.setImageResource(imageResource)
            // shrink imageview
            childIcon.adjustViewBounds = true
            childIcon.maxHeight = 100
            childIcon.maxWidth = 100
            childName.text = text
        }

        return binding.root
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    // change color when groupPosition % 2 == 0
    private fun setLayoutBackgroundColor(groupPosition: Int,layout: ConstraintLayout){
        if (groupPosition % 2 == 0) {
            layout.setBackgroundResource(R.drawable.listview_colours_alt)
        }else{
            layout.setBackgroundResource(R.drawable.listview_colours)
        }
    }

    override fun areAllItemsEnabled(): Boolean {
        return true
    }

    fun getClickedMod(groupPosition: Int, childPosition: Int): ArchnemesisMod{
        return mods[groupPosition].recipe!![childPosition]
    }

    fun getModPosition(key: String): Int?{
        return map[key]
    }

}