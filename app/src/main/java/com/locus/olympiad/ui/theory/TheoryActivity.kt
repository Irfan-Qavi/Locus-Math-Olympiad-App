package com.locus.olympiad.ui.theory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.locus.olympiad.data.dao.TheoryModuleDao
import com.locus.olympiad.databinding.ActivityTheoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TheoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTheoryBinding

    @Inject
    lateinit var theoryModuleDao: TheoryModuleDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTheoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackTheory.setOnClickListener { finish() }

        loadDefaultTheory()
    }

    private fun loadDefaultTheory() {
        lifecycleScope.launch {
            val module = withContext(Dispatchers.IO) {
                theoryModuleDao.getModulesByCategorySync("Combinatorics").firstOrNull()
            }
            if (module != null) {
                binding.tvTheoryTitle.text = module.title
                binding.webViewTheoryMath.setLatexContent(module.latexContent)
            }
        }
    }
}
