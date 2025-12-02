package com.example.myapplication.SignIn;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.myapplication.ResultCallBack;
import com.example.myapplication.UserManager;
import com.example.myapplication.userdata.ChildAccount;
import com.example.myapplication.userdata.ParentAccount;
import com.example.myapplication.userdata.UserData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SignInPresenterTest {

    private SignInView mockView;
    private SignInModel mockModel;
    private SignInPresenter presenter;

    @Before
    public void setUp() {
        mockView = mock(SignInView.class);
        mockModel = mock(SignInModel.class);
        presenter = new SignInPresenter(mockView, mockModel);
        UserManager.currentUser = null;
    }

    // ==================== initialize() Method Tests ====================
    
    @Test
    public void testInitialize_ResetsCurrentUserAndCallsReloadUserAuth() {
        // Set a non-null user to verify initialize() resets it
        UserManager.currentUser = new ParentAccount();
        
        presenter.initialize();
        
        // Verify UserManager.currentUser is set to null
        assertNull(UserManager.currentUser);
        
        // Verify model.ReloadUserAuth() is called
        verify(mockModel).ReloadUserAuth();
    }

    // ==================== signin() Method Tests ====================
    
    @Test
    public void testSignin_WithNullInput1_ShowsError() {
        presenter.signin(null, "password");
        verify(mockView).showShortMessage("input cannot be empty");
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithNullInput2_ShowsError() {
        presenter.signin("email@test.com", null);
        verify(mockView).showShortMessage("input cannot be empty");
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithEmptyInput1_ShowsError() {
        presenter.signin("", "password");
        verify(mockView).showShortMessage("input cannot be empty");
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithEmptyInput2_ShowsError() {
        presenter.signin("email@test.com", "");
        verify(mockView).showShortMessage("input cannot be empty");
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithEmail_CallsSignInForParentAndProvider() {
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
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
        }).when(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signin(email, password);
        
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        
        authCallback.getValue().onComplete(true);
        verify(mockModel).GetCurrentUIDAuth();
        verify(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        assertNotNull("QueryDBforNonChildren callback should be captured", queryCallbackHolder[0]);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        // Verify all callback paths are executed (lines 45, 50, 51-55)
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
        verify(mockModel, never()).usernameExists(anyString(), any());
    }

    @Test
    public void testSignin_WithUsername_CallsSignInForChild() {
        String username = "testuser";
        String password = "password123";
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
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signin(username, password);
        
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        usernameCallback.getValue().onComplete(parentId);
        verify(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        assertNotNull("QueryDBforChildren callback should be captured", queryCallbackHolder[0]);
        queryCallbackHolder[0].onComplete(childAccount);
        
        // Verify all callback paths are executed (lines 81-87)
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
        assertEquals(childAccount, UserManager.currentUser);
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
    }

    @Test
    public void testSignin_WithWhitespaceEmail_TrimsAndCallsParentProvider() {
        String email = "  test@example.com  ";
        String password = "  password123  ";
        String userId = "user123";
        
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
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
        }).when(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signin(email, password);
        
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq("test@example.com"), eq("password123"), authCallback.capture());
        
        // Execute callback to ensure line 30 (password.trim()) and line 32 (email.trim()) are covered
        authCallback.getValue().onComplete(true);
        verify(mockModel).GetCurrentUIDAuth();
        verify(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        assertNotNull("QueryDBforNonChildren callback should be captured", queryCallbackHolder[0]);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        // Verify all callback paths are executed
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
    }

    @Test
    public void testSignin_WithWhitespaceUsername_TrimsAndCallsChild() {
        String username = "  testuser  ";
        String password = "  password123  ";
        String parentId = "parent123";
        
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword("password123");
        childAccount.setID("testuser");
        childAccount.setFirstTime(false);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq("testuser"), any(ResultCallBack.class));
        
        presenter.signin(username, password);
        
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq("testuser"), usernameCallback.capture());
        
        usernameCallback.getValue().onComplete(parentId);
        verify(mockModel).QueryDBforChildren(eq(parentId), eq("testuser"), any(ResultCallBack.class));
        assertNotNull("QueryDBforChildren callback should be captured", queryCallbackHolder[0]);
        queryCallbackHolder[0].onComplete(childAccount);
        
        // Verify all callback paths are executed (lines 30, 35, 81-87)
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
        assertEquals(childAccount, UserManager.currentUser);
        verify(mockModel, never()).SignInAuth(anyString(), anyString(), any());
    }

    // ==================== signInForParentAndProvider() Method Tests ====================
    
    @Test
    public void testSignInForParentAndProvider_SuccessWithFirstTimeTrue_NavigatesToOnboarding() {
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(true);
        parentAccount.setID(userId);
        
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signInForParentAndProvider(email, password);
        
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        
        authCallback.getValue().onComplete(true);
        verify(mockModel).GetCurrentUIDAuth();
        verify(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        assertNotNull("QueryDBforNonChildren callback should be captured", queryCallbackHolder[0]);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToOnBoardingActivity();
        verify(mockView, never()).GoToMainActivity();
    }

    @Test
    public void testSignInForParentAndProvider_SuccessWithFirstTimeFalse_NavigatesToMain() {
        String email = "test@example.com";
        String password = "password123";
        String userId = "user123";
        
        ParentAccount parentAccount = new ParentAccount();
        parentAccount.setFirstTime(false);
        parentAccount.setID(userId);
        
        when(mockModel.GetCurrentUIDAuth()).thenReturn(userId);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(1);
                return null;
            }
        }).when(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        
        presenter.signInForParentAndProvider(email, password);
        
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        
        authCallback.getValue().onComplete(true);
        verify(mockModel).GetCurrentUIDAuth();
        verify(mockModel).QueryDBforNonChildren(eq(userId), any(ResultCallBack.class));
        assertNotNull("QueryDBforNonChildren callback should be captured", queryCallbackHolder[0]);
        queryCallbackHolder[0].onComplete(parentAccount);
        
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
    }

    @Test
    public void testSignInForParentAndProvider_Failure_ShowsError() {
        String email = "test@example.com";
        String password = "wrongpassword";
        
        presenter.signInForParentAndProvider(email, password);
        
        ArgumentCaptor<ResultCallBack<Boolean>> authCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).SignInAuth(eq(email), eq(password), authCallback.capture());
        
        authCallback.getValue().onComplete(false);
        
        verify(mockView).showShortMessage("User Not Found");
        verify(mockView, never()).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
        verify(mockModel, never()).QueryDBforNonChildren(anyString(), any());
    }

    // ==================== signInForChild() Method Tests ====================
    
    @Test
    public void testSignInForChild_EmptyResult_ShowsError() {
        String username = "nonexistent";
        String password = "password123";
        
        presenter.signInForChild(username, password);
        
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        usernameCallback.getValue().onComplete("");
        
        verify(mockView).showShortMessage("User Not Found");
        verify(mockView, never()).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
        verify(mockModel, never()).QueryDBforChildren(anyString(), anyString(), any());
    }

    @Test
    public void testSignInForChild_PasswordMismatch_ShowsError() {
        String username = "childuser";
        String password = "wrongpassword";
        String parentId = "parent123";
        String correctPassword = "correctpassword";
        
        ChildAccount childAccount = new ChildAccount();
        childAccount.setPassword(correctPassword);
        childAccount.setID(username);
        childAccount.setFirstTime(false);
        
        final ResultCallBack<UserData>[] queryCallbackHolder = new ResultCallBack[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                queryCallbackHolder[0] = invocation.getArgument(2);
                return null;
            }
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        usernameCallback.getValue().onComplete(parentId);
        verify(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        assertNotNull("QueryDBforChildren callback should be captured", queryCallbackHolder[0]);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(mockView).showShortMessage("User Not Found");
        verify(mockView, never()).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
    }

    @Test
    public void testSignInForChild_SuccessWithFirstTimeTrue_NavigatesToOnboarding() {
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
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        usernameCallback.getValue().onComplete(parentId);
        verify(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        assertNotNull("QueryDBforChildren callback should be captured", queryCallbackHolder[0]);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToOnBoardingActivity();
        verify(mockView, never()).GoToMainActivity();
        assertEquals(childAccount, UserManager.currentUser);
    }

    @Test
    public void testSignInForChild_SuccessWithFirstTimeFalse_NavigatesToMain() {
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
        }).when(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        
        presenter.signInForChild(username, password);
        
        ArgumentCaptor<ResultCallBack<String>> usernameCallback = ArgumentCaptor.forClass(ResultCallBack.class);
        verify(mockModel).usernameExists(eq(username), usernameCallback.capture());
        
        usernameCallback.getValue().onComplete(parentId);
        verify(mockModel).QueryDBforChildren(eq(parentId), eq(username), any(ResultCallBack.class));
        assertNotNull("QueryDBforChildren callback should be captured", queryCallbackHolder[0]);
        queryCallbackHolder[0].onComplete(childAccount);
        
        verify(mockView).showShortMessage("Welcome!");
        verify(mockView).GoToMainActivity();
        verify(mockView, never()).GoToOnBoardingActivity();
        assertEquals(childAccount, UserManager.currentUser);
    }

    // ==================== isEmail() Method Tests ====================
    
    @Test
    public void testIsEmail_ValidEmails_ReturnsTrue() {
        assertTrue(presenter.isEmail("test@example.com"));
        assertTrue(presenter.isEmail("user.name@domain.co.uk"));
        assertTrue(presenter.isEmail("user+tag@example.org"));
    }

    @Test
    public void testIsEmail_InvalidEmails_ReturnsFalse() {
        assertFalse(presenter.isEmail("notanemail"));
        assertFalse(presenter.isEmail("test@"));
        assertFalse(presenter.isEmail("@example.com"));
        assertFalse(presenter.isEmail("test@example"));
        assertFalse(presenter.isEmail("username"));
    }

    // ==================== isNull() Method Tests ====================
    
    @Test
    public void testIsNull_NullInput_ReturnsTrue() {
        assertTrue(presenter.isNull(null));
    }

    @Test
    public void testIsNull_EmptyString_ReturnsTrue() {
        assertTrue(presenter.isNull(""));
    }

    @Test
    public void testIsNull_NonEmptyString_ReturnsFalse() {
        assertFalse(presenter.isNull("not empty"));
        assertFalse(presenter.isNull(" "));
        assertFalse(presenter.isNull("test"));
    }
}
