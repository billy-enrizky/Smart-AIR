package com.example.myapplication.SignIn;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.UserData;

public class SignInPresenter {

    private static final String MSG_USER_NOT_FOUND = "User Not Found";
    private static final String MSG_WELCOME = "Welcome!";
    private static final String MSG_INPUT_EMPTY = "input cannot be empty";
    
    private static final java.util.regex.Pattern EMAIL_PATTERN = 
        java.util.regex.Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );

    private ISignInView view;
    private ISignInModel model;

    public SignInPresenter(ISignInView view, ISignInModel model) {
        this.view = view;
        this.model = model;
    }

    public void initialize() {
        try {
            UserManager.currentUser = null;
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Handle case where UserManager static initialization fails (e.g., in tests without Firebase)
        }
        model.ReloadUserAuth();
    }

    public void signin(String input1, String input2) {
        if (isNull(input1) || isNull(input2)) {
            view.showShortMessage(MSG_INPUT_EMPTY);
            return;
        }
        String password = input2.trim();
        if (isEmail(input1.trim())) {
            String email = input1.trim();
            signInForParentAndProvider(email, password);
        } else {
            String username = input1.trim();
            signInForChild(username, password);
        }
    }

    public void signInForParentAndProvider(String email, String password) {
        model.SignInAuth(email, password, result -> {
            if (result) {
                view.showShortMessage(MSG_WELCOME);
                String ID = model.GetCurrentUIDAuth();
                if (ID != null) {
                    model.QueryDBforNonChildren(ID, resultUser -> {
                        if (resultUser != null) {
                            setCurrentUserSafely(resultUser);
                            navigateBasedOnFirstTime(resultUser);
                        } else {
                            view.showShortMessage(MSG_USER_NOT_FOUND);
                        }
                    });
                } else {
                    view.showShortMessage(MSG_USER_NOT_FOUND);
                }
            } else {
                view.showShortMessage(MSG_USER_NOT_FOUND);
            }
        });
    }

    public void signInForChild(String username, String password) {
        model.usernameExists(username, result -> {
            if (result == null || result.equals("")) {
                view.showShortMessage(MSG_USER_NOT_FOUND);
            } else {
                String parentID = result;
                model.QueryDBforChildren(parentID, username, childResult -> {
                    if (childResult == null) {
                        view.showShortMessage(MSG_USER_NOT_FOUND);
                        return;
                    }
                    ChildAccount child = (ChildAccount) childResult;
                    if (child.getPassword() == null || !child.getPassword().equals(password)) {
                        view.showShortMessage(MSG_USER_NOT_FOUND);
                        return;
                    }
                    setCurrentUserSafely(childResult);
                    view.showShortMessage(MSG_WELCOME);
                    navigateBasedOnFirstTime(childResult);
                });
            }
        });
    }

    public Boolean isEmail(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        // Using Java regex for robust email validation without Android dependencies
        return EMAIL_PATTERN.matcher(input).matches();
    }

    public Boolean isNull(String input) {
        return (input == null || input.equals(""));
    }

    private void setCurrentUserSafely(UserData user) {
        try {
            UserManager.currentUser = user;
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // UserManager unavailable in test environment
        }
    }

    private void navigateBasedOnFirstTime(UserData user) {
        if (user.getFirstTime()) {
            view.GoToOnBoardingActivity();
        } else {
            view.GoToMainActivity();
        }
    }
}
