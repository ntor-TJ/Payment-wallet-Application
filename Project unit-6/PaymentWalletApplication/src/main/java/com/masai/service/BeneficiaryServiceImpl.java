
package com.masai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.masai.entities.BeneficiaryDetails;
import com.masai.entities.Customer;
import com.masai.entities.Wallet;
import com.masai.exception.BeneficiaryException;
import com.masai.repository.BeneficiaryDao;
import com.masai.repository.WalletDao;

@Service
public class BeneficiaryServiceImpl implements BeneficiaryService {

    @Autowired
    private BeneficiaryDao bDao;

    @Autowired
    private WalletDao wDao;

    @Override
    @Transactional
    public BeneficiaryDetails addBeneficiary(BeneficiaryDetails bd) {

        if (bd == null || bd.getMobileNumber() == null
                || bd.getMobileNumber().isBlank()) {
            throw new BeneficiaryException("Invalid beneficiary details");
        }

        if (bd.getName() == null || bd.getName().isBlank()) {
            throw new BeneficiaryException("Name can't be Empty");
        }

        if (bDao.existsById(bd.getMobileNumber())) {
            throw new BeneficiaryException("Beneficiary already exists");
        }

        if (bd.getWallet() == null || bd.getWallet().getWalletId() == null) {
            throw new BeneficiaryException("Wallet details are required");
        }

        Wallet wallet = wDao.findById(bd.getWallet().getWalletId())
                .orElseThrow(() -> new BeneficiaryException("Wallet not found"));

        bd.setWallet(wallet);

        BeneficiaryDetails saved = bDao.save(bd);

        if (wallet.getBd() == null) {
            wallet.setBd(new ArrayList<>());
        }
        if (!wallet.getBd().contains(saved)) {
            wallet.getBd().add(saved);
        }

        return saved;
    }

    @Override
    public BeneficiaryDetails viewBeneficiary(String mobNo) {
        if (mobNo == null || mobNo.isBlank()) {
            throw new BeneficiaryException("Invalid mobile number");
        }

        return bDao.findById(mobNo)
                .orElseThrow(() -> new BeneficiaryException("Beneficiary Not found"));
    }

    @Override
    public List<BeneficiaryDetails> viewAllBeneficiary(Customer customer) {

        if (customer == null || customer.getWallet() == null
                || customer.getWallet().getWalletId() == null) {
            throw new BeneficiaryException("Wallet details are required");
        }

        Integer id = customer.getWallet().getWalletId();

        Wallet wallet = wDao.findById(id)
                .orElseThrow(() -> new BeneficiaryException("Wallet not found"));

        if (wallet.getBd() == null) {
            return new ArrayList<>();
        }

        return wallet.getBd();
    }

    @Override
    @Transactional
    public BeneficiaryDetails deleteBeneficiary(BeneficiaryDetails bd) {

        if (bd == null || bd.getMobileNumber() == null || bd.getMobileNumber().isBlank()) {
            throw new BeneficiaryException("Invalid beneficiary details");
        }

        BeneficiaryDetails beneficiary = bDao.findById(bd.getMobileNumber())
                .orElseThrow(() -> new BeneficiaryException(
                        "No such Beneficiary present to delete"));

        Wallet wallet = beneficiary.getWallet();
        if (wallet != null && wallet.getBd() != null) {
            wallet.getBd().remove(beneficiary);
        }

        bDao.delete(beneficiary);

        return beneficiary;
    }
}
