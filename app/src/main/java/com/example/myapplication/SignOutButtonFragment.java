package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.SignIn.SignInView;
import com.example.myapplication.userdata.AccountType;
import com.example.myapplication.userdata.ChildAccount;
import com.google.firebase.auth.FirebaseAuth;

public class SignOutButtonFragment extends Fragment {

    public SignOutButtonFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_signoutbutton, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button signOutButton = view.findViewById(R.id.button4);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(UserManager.currentUser.getAccount() == AccountType.CHILD){
                    ChildAccount child = (ChildAccount) UserManager.currentUser;
                    UserManager.stopChildUserListener(child.getParent_id(),child.getID());
                }else{
                    UserManager.stopUserListener(UserManager.currentUser.getID());
                }
                UserManager.currentUser = null;
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), SignInView.class);
                startActivity(intent);
                getActivity().finish();
            }
        });
    }
}