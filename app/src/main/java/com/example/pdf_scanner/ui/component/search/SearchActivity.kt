package com.example.pdf_scanner.ui.component.search

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pdf_scanner.JPG
import com.example.pdf_scanner.KEY_DATA_DETAIL
import com.example.pdf_scanner.KEY_DATA_SEARCH
import com.example.pdf_scanner.R
import com.example.pdf_scanner.data.Resource
import com.example.pdf_scanner.data.dto.DataSearch
import com.example.pdf_scanner.data.dto.ImageFolder
import com.example.pdf_scanner.data.dto.OBase
import com.example.pdf_scanner.databinding.ActivitySearchBinding
import com.example.pdf_scanner.ui.base.BaseActivity
import com.example.pdf_scanner.ui.base.listener.RecyclerItemListener
import com.example.pdf_scanner.ui.component.detail.DetailActivity
import com.example.pdf_scanner.ui.component.search.adapter.CardFolderAdapter
import com.example.pdf_scanner.utils.FileUtil
import com.example.pdf_scanner.utils.toObject
import com.oneadx.vpnclient.utils.observe
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class SearchActivity : BaseActivity() {

    lateinit var binding: ActivitySearchBinding
    val viewModel: SearchViewModel by viewModels()
    lateinit var adapter: CardFolderAdapter
    var list = ArrayList<ImageFolder>()

    override fun initViewBinding() {
        binding = ActivitySearchBinding.inflate(layoutInflater)

        list = intent.getStringExtra(KEY_DATA_SEARCH)!!.toObject<DataSearch>().list

        binding.btnDeleteSearch.setOnClickListener {
            finish()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.edtSearch.setText("", TextView.BufferType.EDITABLE)
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.animSearch.setAnimation(R.raw.empty_box)
        binding.animSearch.playAnimation()

        adapter = CardFolderAdapter(object : RecyclerItemListener {
            override fun onItemSelected(index: Int, data: OBase) {
                var o = data as ImageFolder
                var intent = Intent(this@SearchActivity, DetailActivity::class.java)
                intent.putExtra(KEY_DATA_DETAIL, o.toJSON())
                startActivityForResult(intent, 1)
            }

            override fun onOption(index: Int, data: OBase) {

            }
        })

        binding.rcclvSearch.layoutManager = LinearLayoutManager(this)
        binding.rcclvSearch.adapter = adapter
        viewModel.fetchData(list)
        setContentView(binding.root)
    }

    override fun observeViewModel() {
        observe(viewModel.listFolder, ::handleFolder)
    }

    private fun handleFolder(data: Resource<ArrayList<ImageFolder>>) {
        when (data) {
            is Resource.Success -> {
                var list = data.data!!
                adapter.setData(list)
                if (list.size == 0 && binding.layoutFolderSearchEmpty.visibility == View.GONE) {
                    binding.layoutFolderSearchEmpty.visibility = View.VISIBLE
                } else if (list.size != 0 && binding.layoutFolderSearchEmpty.visibility == View.VISIBLE) {
                    binding.layoutFolderSearchEmpty.visibility = View.GONE
                }

            }
        }
    }

    private fun fetchFolder() {
        var list = ArrayList<ImageFolder>()
        var pathSaved = "/saved"
        var filePath = FileUtil(this@SearchActivity).getRootFolder() + pathSaved
        val directory = File(filePath)
        if (!directory.exists() || directory.listFiles() == null) {
            return
        }
        val files: Array<File> = directory.listFiles().reversedArray()
        for (i in files.indices) {
            var listPath = ArrayList<String>()
            if (files[i].listFiles() == null) {
                continue
            }
            for (f in files[i].listFiles()) {
                if (f.name.endsWith(JPG)) {
                    listPath.add(f.path)
                }
            }
            if (listPath.size == 0) {
                files[i].delete()
                continue
            }

            var fileTimeCreated = SimpleDateFormat("yyyy/MM/dd").format(
                Date(files[i].lastModified())
            );
            list.add(ImageFolder(files[i].name, fileTimeCreated, listPath))
        }
        viewModel.fetchData(list)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fetchFolder()
        binding.edtSearch.setText("")
        super.onActivityResult(requestCode, resultCode, data)
    }
}