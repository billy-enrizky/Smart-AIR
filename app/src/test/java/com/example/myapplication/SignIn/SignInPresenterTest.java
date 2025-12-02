package com.example.myapplication.SignIn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.myapplication.userdata.ParentAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

public class SignInPresenterTest {
    @Mock
    SignInView view;

    @Mock
    SignInModel model;

    @Mock
    FirebaseAuth mAuth;
    @Mock
    DatabaseReference mDatabase;

    private MockedStatic<FirebaseAuth> firebaseAuthStatic;
    private MockedStatic<Process> processStatic;

    @Before
    public void setUp() {
        processStatic = mockStatic(Process.class);
        firebaseAuthStatic = mockStatic(FirebaseAuth.class);
        firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mAuth);
    }
    @Test
    public void testSignInPresenter1() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertEquals(presenter.view, view);
    }
    @Test
    public void testSignInPresenter2() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertEquals(presenter.model, model);
    }
    @Test
    public void testInitialize_ResetsCurrentUserToNull() {
        com.example.myapplication.UserManager.currentUser = new ParentAccount();
        SignInPresenter presenter = new SignInPresenter(view, model);
        model.mAuth = mAuth;
        model.mDatabase = mDatabase;
        when(mAuth.getCurrentUser()).thenReturn(null);
        presenter.initialize();
        assertNull(com.example.myapplication.UserManager.currentUser);
    }

    @Test
    public void testInitialize_CallsReloadUserAuth() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        presenter.initialize();
        verify(model).ReloadUserAuth();
    }

    /*
    @Test
    public void testSignin_WithNullInput1_ShowsErrorMessage() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        presenter.signin(null, "password");
        verify(view).showShortMessage("input cannot be empty");
    }

    @Test
    public void testSignin_WithNullInput1_DoesNotCallSignInAuth() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        presenter.signin(null, "password");
        verify(model, never()).SignInAuth(anyString(), anyString(), any());
    }

    @Test
    public void testSignin_WithNullInput1_DoesNotCallUsernameExists() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        presenter.signin(null, "password");
        verify(model, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithNullInput2_ShowsErrorMessage() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        presenter.signin("email@test.com", null);
        verify(view).showShortMessage("input cannot be empty");
    }

    @Test
    public void testSignin_WithEmptyInput1_ShowsErrorMessage() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        presenter.signin("", "password");
        verify(view).showShortMessage("input cannot be empty");
    }

    @Test
    public void testSignin_WithEmptyInput2_ShowsErrorMessage() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        presenter.signin("email@test.com", "");
        verify(view).showShortMessage("input cannot be empty");
    }

    @Test
    public void testSignin_WithEmail_CallsSignInAuth() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        presenter.signin(email, password);
        verify(model).SignInAuth(eq(email), eq(password), any());
    }

    @Test
    public void testSignin_WithEmail_DoesNotCallUsernameExists() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        presenter.signin(email, password);
        verify(model, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithUsername_CallsUsernameExists() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "testuser";
        String password = "password123";
        presenter.signin(username, password);
        verify(model).usernameExists(eq(username), any());
    }

    @Test
    public void testSignin_WithUsername_DoesNotCallSignInAuth() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "testuser";
        String password = "password123";
        presenter.signin(username, password);
        verify(model, never()).SignInAuth(anyString(), anyString(), any());
    }

    @Test
    public void testSignin_WithWhitespaceEmail_TrimsEmail() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "  test@example.com  ";
        String password = "password123";
        presenter.signin(email, password);
        verify(model).SignInAuth(eq("test@example.com"), eq(password), any());
    }

    @Test
    public void testSignin_WithWhitespacePassword_TrimsPassword() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "  password123  ";
        presenter.signin(email, password);
        verify(model).SignInAuth(eq(email), eq("password123"), any());
    }

    @Test
    public void testSignin_WithWhitespaceUsername_TrimsUsername() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "  testuser  ";
        String password = "password123";
        presenter.signin(username, password);
        verify(model).usernameExists(eq("testuser"), any());
    }
    @Test
    public void testSignInForParentAndProvider_CallsSignInAuth() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        presenter.signInForParentAndProvider(email, password);
        verify(model).SignInAuth(eq(email), eq(password), any());
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthSuccess_CallsGetCurrentUIDAuth1() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(model.GetCurrentUIDAuth()).thenReturn(userId);
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).SignInAuth(eq(email), eq(password), authCallback.capture());
    }
    @Test
    public void testSignInForParentAndProvider_OnAuthSuccess_CallsGetCurrentUIDAuth2() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(model.GetCurrentUIDAuth()).thenReturn(userId);
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        authCallback.getValue().onComplete(true);
        verify(model).GetCurrentUIDAuth();
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthSuccess_CallsQueryDBforNonChildren1() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(model.GetCurrentUIDAuth()).thenReturn(userId);
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).SignInAuth(eq(email), eq(password), authCallback.capture());
    }
    @Test
    public void testSignInForParentAndProvider_OnAuthSuccess_CallsQueryDBforNonChildren2() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(model.GetCurrentUIDAuth()).thenReturn(userId);
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        authCallback.getValue().onComplete(true);
        verify(model).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthSuccess_ShowsWelcomeMessage() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(model.GetCurrentUIDAuth()).thenReturn(userId);
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(false);
        parentAccount.setID(userId);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(model).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(true);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        verify(view).showShortMessage("Welcome!");
    }

    @Test
    public void testSignInForParentAndProvider_FirstTimeTrue_NavigatesToOnboarding() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(model.GetCurrentUIDAuth()).thenReturn(userId);
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(true);
        parentAccount.setID(userId);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(model).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(true);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        verify(view).GoToOnBoardingActivity();
    }

    @Test
    public void testSignInForParentAndProvider_FirstTimeFalse_NavigatesToMain() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        when(model.GetCurrentUIDAuth()).thenReturn(userId);
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(false);
        parentAccount.setID(userId);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(model).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(true);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        verify(view).GoToMainActivity();
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthFailure_ShowsErrorMessage() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "wrongpassword";
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(false);
        verify(view).showShortMessage("User Not Found");
    }

    @Test
    public void testSignInForParentAndProvider_OnAuthFailure_DoesNotCallQueryDB() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String email = "test@example.com";
        String password = "wrongpassword";
        presenter.signInForParentAndProvider(email, password);
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).SignInAuth(eq(email), eq(password), authCallback.capture());
        authCallback.getValue().onComplete(false);
        verify(model, never()).QueryDBforNonChildren(anyString(), any());
    }
    
    @Test
    public void testSignInForChild_CallsUsernameExists() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "testuser";
        String password = "password123";
        presenter.signInForChild(username, password);
        verify(model).usernameExists(eq(username), any());
    }

    @Test
    public void testSignInForChild_EmptyResult_ShowsErrorMessage() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "nonexistent";
        String password = "password123";
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete("");
        verify(view).showShortMessage("User Not Found");
    }
/*
    @Test
    public void testSignInForChild_EmptyResult_DoesNotCallQueryDB() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "nonexistent";
        String password = "password123";
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete("");
        verify(model, never()).QueryDBforChildren(anyString(), anyString(), any());
    }

    @Test
    public void testSignInForChild_NonEmptyResult_CallsQueryDBforChildren() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "childuser";
        String password = "password123";
        String parentId = "parent123";
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        verify(model).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
    }

    @Test
    public void testSignInForChild_PasswordMismatch_ShowsErrorMessage() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "childuser";
        String password = "wrongpassword";
        String parentId = "parent123";
        String correctPassword = "correctpassword";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(correctPassword);
        childAccount.setID(username);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(model).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(view).showShortMessage("User Not Found");
    }

    @Test
    public void testSignInForChild_Success_SetsCurrentUser() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(model).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        assertEquals(childAccount, UserManager.currentUser);
    }

    @Test
    public void testSignInForChild_Success_ShowsWelcomeMessage() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(model).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(view).showShortMessage("Welcome!");
    }

    @Test
    public void testSignInForChild_FirstTimeTrue_NavigatesToOnboarding() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        childAccount.setFirstTime(true);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(model).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(view).GoToOnBoardingActivity();
    }

    @Test
    public void testSignInForChild_FirstTimeFalse_NavigatesToMain() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        String username = "childuser";
        String password = "correctpassword";
        String parentId = "parent123";
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(password);
        childAccount.setID(username);
        childAccount.setFirstTime(false);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(model).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(model).usernameExists(eq(username), usernameCallback.capture());
        usernameCallback.getValue().onComplete(parentId);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(view).GoToMainActivity();
    }

    
    @Test
    public void testIsEmail_ValidEmail1_ReturnsTrue() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertTrue(presenter.isEmail("test@example.com"));
    }

    @Test
    public void testIsEmail_ValidEmail2_ReturnsTrue() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertTrue(presenter.isEmail("user.name@domain.co.uk"));
    }

    @Test
    public void testIsEmail_ValidEmail3_ReturnsTrue() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertTrue(presenter.isEmail("user+tag@example.org"));
    }

    @Test
    public void testIsEmail_InvalidEmail1_ReturnsFalse() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertFalse(presenter.isEmail("notanemail"));
    }

    @Test
    public void testIsEmail_InvalidEmail2_ReturnsFalse() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertFalse(presenter.isEmail("test@"));
    }

    @Test
    public void testIsEmail_InvalidEmail3_ReturnsFalse() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertFalse(presenter.isEmail("@example.com"));
    }

    @Test
    public void testIsEmail_InvalidEmail4_ReturnsFalse() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertFalse(presenter.isEmail("test@example"));
    }

    @Test
    public void testIsEmail_InvalidEmail5_ReturnsFalse() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertFalse(presenter.isEmail("username"));
    }
    
    @Test
    public void testIsNull_NullInput_ReturnsTrue() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertTrue(presenter.isNull(null));
    }

    @Test
    public void testIsNull_EmptyString_ReturnsTrue() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertTrue(presenter.isNull(""));
    }

    @Test
    public void testIsNull_NonEmptyString1_ReturnsFalse() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertFalse(presenter.isNull("not empty"));
    }

    @Test
    public void testIsNull_NonEmptyString2_ReturnsFalse() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertFalse(presenter.isNull(" "));
    }

    @Test
    public void testIsNull_NonEmptyString3_ReturnsFalse() {
        SignInPresenter presenter = new SignInPresenter(view, model);
        assertFalse(presenter.isNull("test"));
    }*/
}

