package com.locus.olympiad.ui.theory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.locus.olympiad.data.dao.TheoryModuleDao
import com.locus.olympiad.databinding.ActivityTheoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TheoryFragment : Fragment() {

    private var _binding: ActivityTheoryBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var theoryModuleDao: TheoryModuleDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityTheoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackTheory.setOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        loadDefaultTheory()
    }

    private fun loadDefaultTheory() {
        viewLifecycleOwner.lifecycleScope.launch {
            val module = withContext(Dispatchers.IO) {
                theoryModuleDao.getModulesByCategorySync("Combinatorics").firstOrNull()
            }
            if (_binding != null && module != null) {
                binding.tvTheoryTitle.text = module.title
                binding.webViewTheoryMath.setLatexContent(module.latexContent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
