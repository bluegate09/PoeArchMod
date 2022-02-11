package chingyu.poearchnemesismods

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import chingyu.poearchnemesismods.databinding.RewardFilterLayoutBinding

class RewardFilterDialog : DialogFragment() {

    private lateinit var binding: RewardFilterLayoutBinding
    private lateinit var dataPasser: OnDataPass

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataPasser = context as OnDataPass
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater,R.layout.reward_filter_layout,container,false)
        binding.dataPasser = this

        return binding.root
    }

    fun onDataPass(tag: Int){
        dataPasser.onDataPass(tag)
        dismiss()
    }

}

interface OnDataPass {
    fun onDataPass(tag: Int)
}