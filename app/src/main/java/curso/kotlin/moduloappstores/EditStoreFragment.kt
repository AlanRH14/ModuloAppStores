package curso.kotlin.moduloappstores

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import curso.kotlin.moduloappstores.databinding.FragmentEditStoreBinding
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class EditStoreFragment : Fragment() {
    private lateinit var binding: FragmentEditStoreBinding
    private var mActivity: MainActivity? = null
    private var mIsEditMode: Boolean = false
    private var mStoreEntity: StoreEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditStoreBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = arguments?.getLong(getString(R.string.arg_id), 0)
        if (id != null && id != 0L) {
            mIsEditMode = true
            getStore(id)
        } else {
            mIsEditMode = false
            mStoreEntity = StoreEntity(name = "", phone = "", photoUrl = "")
        }

        setupActionBar()
        setupTextFields()
    }

    private fun setupActionBar() {
        mActivity = activity as? MainActivity
        mActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mActivity?.supportActionBar?.title =
            if (mIsEditMode) getString(R.string.edit_store_title_edit)
            else getString(R.string.edit_store_title_add)

        setHasOptionsMenu(true)
    }

    private fun setupTextFields() {

        with(binding) {
            edtName.addTextChangedListener { validateFields(tilName) }
            edtPhone.addTextChangedListener { validateFields(tilPhone) }
            edtPhotoUrl.addTextChangedListener {
                validateFields(tilPhotoUrl)
                loadImage(it.toString().trim())
            }
        }
    }

    private fun loadImage(url: String) {
        Glide.with(this)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(binding.imgPhoto)
    }

    private fun getStore(id: Long) {
        doAsync {
            mStoreEntity = StoreApplication.database.storeDao().getStoreById(id)

            uiThread { if (mStoreEntity != null) setUiStore(mStoreEntity!!) }
        }
    }

    private fun setUiStore(storeEntity: StoreEntity) {
        with(binding) {
            edtName.text = storeEntity.name.editable()
            edtPhone.text = storeEntity.phone.editable()
            edtWebSite.text = storeEntity.webSite.editable()
            edtPhotoUrl.text = storeEntity.photoUrl.editable()
        }
    }

    private fun String.editable(): Editable = Editable.Factory.getInstance().newEditable(this)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_save, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mActivity?.onBackPressed()
                true
            }

            R.id.action_save -> {
                if (mStoreEntity != null && validateFields(
                        binding.tilPhotoUrl,
                        binding.tilPhone,
                        binding.tilName
                    )
                ) {
                    with(mStoreEntity!!) {
                        name = binding.edtName.text.toString().trim()
                        phone = binding.edtPhone.text.toString().trim()
                        webSite = binding.edtWebSite.text.toString().trim()
                        photoUrl = binding.edtPhotoUrl.text.toString().trim()
                    }

                    doAsync {
                        if (mIsEditMode) StoreApplication.database.storeDao()
                            .updateStore(mStoreEntity!!)
                        else
                            mStoreEntity!!.id =
                                StoreApplication.database.storeDao().addStore(mStoreEntity!!)

                        uiThread {
                            hideKeyboard()

                            if (mIsEditMode) {
                                mActivity?.updateStore(mStoreEntity!!)

                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.edit_store_message_update_success),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            } else {
                                mActivity?.addStore(mStoreEntity!!)

                                Toast.makeText(
                                    mActivity,
                                    getString(R.string.edit_store_message_save_success),
                                    Toast.LENGTH_SHORT
                                ).show()

                                mActivity?.onBackPressed()
                            }
                        }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun validateFields(vararg textFields: TextInputLayout): Boolean {
        var isValid = true

        for (textField in textFields) {
            if (textField.editText?.text.toString().trim().isEmpty()) {
                textField.error = getString(R.string.helper_required)
                textField.editText!!.requestFocus()
                isValid = false
            } else
                textField.error = null
        }

        if (!isValid)
            Snackbar.make(
                binding.root,
                getString(R.string.edit_store_message_valid),
                Snackbar.LENGTH_SHORT
            ).show()

        return isValid
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (binding.edtPhotoUrl.text.toString().trim().isEmpty()) {
            binding.tilPhotoUrl.error = getString(R.string.helper_required)
            binding.edtPhotoUrl.requestFocus()

            isValid = false
        }

        if (binding.edtPhone.text.toString().trim().isEmpty()) {
            binding.tilPhone.error = getString(R.string.helper_required)
            binding.edtPhone.requestFocus()

            isValid = false
        }

        if (binding.edtName.text.toString().trim().isEmpty()) {
            binding.tilName.error = getString(R.string.helper_required)
            binding.edtName.requestFocus()

            isValid = false
        }

        return isValid
    }

    private fun hideKeyboard() {
        val imn = mActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imn != null) {
            imn.hideSoftInputFromWindow(view!!.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        hideKeyboard()
        super.onDestroyView()
    }

    override fun onDestroy() {
        mActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        mActivity?.supportActionBar?.title = getString(R.string.app_name)
        mActivity?.hideFab(true)

        setHasOptionsMenu(false)
        super.onDestroy()
    }
}