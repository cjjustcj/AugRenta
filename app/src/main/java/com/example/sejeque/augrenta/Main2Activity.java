package com.example.sejeque.augrenta;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;



public class Main2Activity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    ActionBarDrawerToggle actionBarDrawerToggle;
    Toolbar toolbar;

    String ownerId, propertyId;

    /// Database
    //initiate database reference
    private DatabaseReference requestDatabase, notifDatabase;
    private DatabaseReference mDatabase;
    private StorageReference storageReference;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    //array container for data fetched from firebase database
    List<Property> properties;

    TextView property_name, property_price, property_description,
            property_type, property_area, property_bedroom, property_bathroom, property_pet;

    RelativeLayout bottomBarPanel;
    Button message_owner, request_visitBtn, startToAr;
    CheckBox sunday, monday, tuesday, wednesday, thursday, friday, saturday;
    String prop_name;

    private TextView userNameHandler, emailHandler;
    private ImageView imgHandler;

    String sender, senderID;
    String REQUEST_STATE = "not sent";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Property Details");

        //function for Drawer toggle when clicking menu icon
        //function for Drawer toggle when clicking menu icon
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout1);
        NavigationView sideNavBar = findViewById(R.id.sideNav);
        //get reference for header in navigation view
        View headerView = sideNavBar.getHeaderView(0);

        //instantiate textView and imageView in header of navigation view
        userNameHandler = headerView.findViewById(R.id.textUser);
        emailHandler = headerView.findViewById(R.id.textEmail);
        imgHandler = headerView.findViewById(R.id.imageProfPic);

        //change texts for user's name, email, and profile pic in header of navigation view
        setCredentialView();

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.oper_drawer, R.string.close_drawer);
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        //if an item is clicked on navigation view
        sideNavBar.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemID = item.getItemId();

                if(itemID == R.id.navigation_home){
                    goToHome();
                }
                else if(itemID == R.id.properties){
                    goToPropertyList();
                }
                else if(itemID == R.id.requests){
                    goToRequests();
                }
                else if(itemID == R.id.messenger){
                    goToMessages();
                }
                else if(itemID == R.id.signOut){
                    signOutUser();
                }

                return true;
            }
        });

        // End of Setting Toolbar and Navigation Drawer

        //end of Drawer toggle function

        //get value from ProperyActivity to show Information
        //ownerId = getIntent().getExtras().getString("ownerId");
        propertyId = getIntent().getExtras().getString("propertyId");

        //get reference from firebase database with child node Property
        mDatabase = FirebaseDatabase.getInstance().getReference("Property");
        storageReference = FirebaseStorage.getInstance().getReference("PropertyImages");

        //get user information
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        //TextView widgets initialisation
        property_name = findViewById(R.id.property_name);
        property_price = findViewById(R.id.property_price);
        property_description = findViewById(R.id.property_description);
        property_area = findViewById(R.id.property_area);
        property_type = findViewById(R.id.property_type);
        property_bedroom = findViewById(R.id.property_bedroom);
        property_bathroom = findViewById(R.id.property_bathroom);
        property_pet = findViewById(R.id.property_pets);


        // House seeker request
        requestDatabase = FirebaseDatabase.getInstance().getReference().child("Requests");
        notifDatabase = FirebaseDatabase.getInstance().getReference().child("Notifications");
        notifDatabase.keepSynced(true);

        sender = currentUser.getDisplayName();
        senderID = currentUser.getUid();
        //checkDatabase = FirebaseDatabase.getInstance().getReference().child("Requests").child(senderID).child(ownerId).child(propertyId);

        message_owner = (Button) findViewById(R.id.message_owner);

        message_owner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent starIntent = new Intent(Main2Activity.this, ChatMessage.class);
                starIntent.putExtra("propertyId", propertyId);
                starIntent.putExtra("ownerId", ownerId);
                startActivity(starIntent);
            }
        });

        bottomBarPanel = findViewById(R.id.bottom_bar);


        sunday = findViewById(R.id.checkBoxSun);
        monday = findViewById(R.id.checkBoxMon);
        tuesday = findViewById(R.id.checkBoxTue);
        wednesday = findViewById(R.id.checkBoxWed);
        thursday = findViewById(R.id.checkBoxThu);
        friday = findViewById(R.id.checkBoxFri);
        saturday = findViewById(R.id.checkBoxSat);

        sunday.setOnClickListener(checkboxClickListener);
        monday.setOnClickListener(checkboxClickListener);
        tuesday.setOnClickListener(checkboxClickListener);
        wednesday.setOnClickListener(checkboxClickListener);
        thursday.setOnClickListener(checkboxClickListener);
        friday.setOnClickListener(checkboxClickListener);
        saturday.setOnClickListener(checkboxClickListener);

        startToAr = findViewById(R.id.startAR);
        request_visitBtn = findViewById(R.id.request_visit);
        request_visitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(REQUEST_STATE.equals("not sent")){
                    request_dialog();
                }else if(REQUEST_STATE.equals("sent")){
                    cancelVisitRequest();
                }
            }
        });

    }


        Button direction_tour = (Button) findViewById(R.id.inside_tour);
        direction_tour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent starIntent = new Intent(Main2Activity.this, AugmentActivity.class);
                starIntent.putExtra("propertyId", propertyId);
                startActivity(starIntent);
            }
        });

        indoor_tour = findViewById(R.id.view_indoor_ar);
        indoor_tour.setVisibility(View.GONE);
        indoor_tour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent starIntent = new Intent(Main2Activity.this, AugmentIndoorActivity.class);
                starIntent.putExtra("propertyId", propertyId);
                startActivity(starIntent);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        mDatabase.child(propertyId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showData(dataSnapshot);
                //Toast.makeText(Main2Activity.this, ""+dataSnapshot, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void showData(DataSnapshot dataSnapshot) {
        //for(DataSnapshot propertySnapshot : dataSnapshot.getChildren()){

            Property property = dataSnapshot.getValue(Property.class);
            //Toast.makeText(this, ""+property.propertyName, Toast.LENGTH_SHORT).show();

            ownerId = property.owner;
            property_name.setText(property.propertyName);
            property_price.setText(property.price + " PHP");
            property_description.setText(property.description);
            property_area.setText(property.area);
            property_type.setText(property.type);
            property_bedroom.setText(property.rooms);
            property_bathroom.setText(property.bathroom);
            property_pet.setText(property.pets);

            prop_name = property.propertyName.toUpperCase();
            String currentId = currentUser.getUid();
            String propertyOwner = property.owner;
            Toast.makeText(this, "Current ID: " + currentId + "\nOwner ID: " + propertyOwner, Toast.LENGTH_SHORT).show();
            if(propertyOwner.equals(currentId)){
                indoor_tour.setVisibility(View.VISIBLE);
            }

            checkCredentials();
        //}
    }

    // Disable Message Button if current User is also the property owner
    private void checkCredentials() {
        if(currentUser.getUid().equals(ownerId)){
            message_owner.setVisibility(View.GONE);
            bottomBarPanel.setVisibility(View.GONE);
        }

        requestDatabase.child(senderID).child(ownerId).child(propertyId).
                addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            RequestVisit req = dataSnapshot.getValue(RequestVisit.class);

                            if (req != null && !req.isAccepted()) {
                                if (req.getType().equals("sender")) {
                                    request_visitBtn.setBackground(getResources().getDrawable(R.drawable.button_placeviewer));
                                    request_visitBtn.setTextColor(getResources().getColor(R.color.white));
                                    REQUEST_STATE = "sent";
                                } else {
                                    Log.d("Requests", "You sent this request");
                                }
                            }else{
                                startToAr.setVisibility(View.VISIBLE);
                                request_visitBtn.setVisibility(View.GONE);
                            }
                        }  else{ Log.d("Requests", "Request not found");}
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });


    }

    private void cancelVisitRequest() {
        requestDatabase.child(senderID).child(ownerId).child(propertyId).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            requestDatabase.child(ownerId).child(senderID).child(propertyId).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        Toast.makeText(Main2Activity.this, "Your Request has been cancelled", Toast.LENGTH_SHORT).show();
                                        request_visitBtn.setEnabled(true);
                                        request_visitBtn.setText("Request Visit");
                                        request_visitBtn.setBackground(getResources().getDrawable(R.drawable.button_placeviewer));
                                        request_visitBtn.setTextColor(getResources().getColor(R.color.white));
                                        REQUEST_STATE = "not sent";
                                    }
                                }
                            });
                        }
                    }
                });
    }

    String timeRequest = "", format="", formattedDate="";
    // Dialog box when user request to visit
    private void request_dialog() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.requestvisit_datetime, null);

        alertDialog.setTitle("Request to Owner");
        alertDialog.setView(view);

        final DatePicker request_datePicker = view.findViewById(R.id.request_datePicker);
        final TimePicker request_timePicker = view.findViewById(R.id.request_timePicker);

        alertDialog.setPositiveButton("Request Visit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //passing value and directing to the Property Information

                int   day  = request_datePicker.getDayOfMonth();
                int   month= request_datePicker.getMonth();
                int   year = request_datePicker.getYear();

                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day);

                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
                formattedDate = sdf.format(calendar.getTime());
                Log.d("Date Format", formattedDate);

                //You can parse the String back to Date object by calling
                //Date date = sdf.parse(formatedDate);

                int hour = request_timePicker.getCurrentHour();
                int min = request_timePicker.getCurrentMinute();
                showTime(hour, min);

                Log.d("Date Format", timeRequest);
                Toast.makeText(Main2Activity.this, ""+formattedDate + " and " + timeRequest, Toast.LENGTH_SHORT).show();

                sendRequestVisit();
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        AlertDialog alertDialogInfo = alertDialog.create();
        alertDialogInfo.show();
    }

    private void sendRequestVisit() {

        final String type1 = "sender", type2 = "receiver";
//        String message_sender_ref = "Requests/" + senderID + "/" + ownerId + "/" + propertyId;
//        String message_receiver_ref = "Requests/" + ownerId + "/" + senderID  + "/" + propertyId;
        RequestVisit request = new RequestVisit(sender, formattedDate, timeRequest, type1, false);
        final RequestVisit request2 = new RequestVisit(sender, formattedDate, timeRequest, type2, false);

        requestDatabase.child(senderID).child(ownerId).child(propertyId).setValue(request)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(Main2Activity.this, "Request Sent", Toast.LENGTH_SHORT).show();
                    requestDatabase.child(ownerId).child(senderID).child(propertyId).setValue(request2)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){

                                        HashMap<String, String> notificationData = new HashMap<String, String>();
                                        notificationData.put("from", sender);
                                        notificationData.put("fromID", senderID );
                                        notificationData.put("type", type1);

                                        notifDatabase.child(ownerId).push().setValue(notificationData)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()){
                                                    request_visitBtn.setBackground(getResources().getDrawable(R.drawable.button_actionplaceviewer));
                                                    request_visitBtn.setText("Cancel Request");
                                                    request_visitBtn.setTextColor(getResources().getColor(R.color.alizarin));
                                                    REQUEST_STATE = "sent";
                                                }
                                            }
                                        });

                                    }
                                }
                            });
                }
            }
        });
    }


    public void showTime(int hour, int min) {

        String minute;

        if(min < 10) {
            minute = "0"+min;
        } else {
            minute = "" + min;
        }
        if (hour == 0) {
            hour += 12;
            format = "AM";
        } else if (hour == 12) {
            format = "PM";
        } else if (hour > 12) {
            hour -= 12;
            format = "PM";
        } else {
            format = "AM";
        }
        timeRequest = hour + ":" + minute + " " + format;

    }



    /*
    *  SIDE NAVBAR METHODS
    *  ONCLICK EVENTS OF ITEMS ON SIDENAVBAR
    *
    */

    //method for setting texts in header in navigation view
    private void setCredentialView() {
        //get user information
        String name = currentUser.getDisplayName();
        String email = currentUser.getEmail();
        Uri photoUrl = currentUser.getPhotoUrl();

        //set text in header in navigation view
        userNameHandler.setText(name);
        emailHandler.setText(email);

        //Picasso turns photoUrl to bitmap
        //then changes the pic in header in navigation view
        Picasso.get().load(photoUrl).into(imgHandler);
    }

    //method for refreshing MapsActivity
    private void goToHome() {
        finish();
        Intent onPropertyView = new Intent(Main2Activity.this, MainActivity.class);
        startActivity(onPropertyView);
    }

    //method for starting PropertyActivity
    private void goToPropertyList() {
        finish();
        Intent onPropertyView = new Intent(Main2Activity.this, PropertyActivity.class);
        startActivity(onPropertyView);
    }

    private void goToRequests() {
        finish();
        Intent onPropertyView = new Intent(Main2Activity.this, SeekerRequestsActivity.class);
        startActivity(onPropertyView);
    }
    private void goToMessages() {
        finish();
        Intent onPropertyView = new Intent(Main2Activity.this, MessengerActivity.class);
        startActivity(onPropertyView);
    }

    //method for signing out current user
    //then going back to login panel
    private void signOutUser() {
        mAuth.signOut();
        LoginManager.getInstance().logOut();
        Toast.makeText(Main2Activity.this, "You have been logout", Toast.LENGTH_SHORT).show();
        finishAffinity();
        proceed();
    }

    private void selectFragment(MenuItem item) {
        Fragment frag = null;
        // init corresponding fragment
        switch (item.getItemId()) {
            case R.id.navigation_person:
                Intent onUserView = new Intent(Main2Activity.this, UserPanelActivity.class);
                startActivity(onUserView);
        }
    }



     /*
    *  UTILITES
    *
    *
    */

    //method for going back to login panel
    private void proceed() {
        finish();
        Intent onReturnView = new Intent(Main2Activity.this, MainActivity.class);
        startActivity(onReturnView);
    }

    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }


    View.OnClickListener checkboxClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean checked = ((CheckBox) view).isChecked();
            String text = null;
            switch (view.getId()) {
                case R.id.checkBoxSun:
                    if(checked){
                        RelativeLayout relSunday = findViewById(R.id.relPanelSunday);
                        relSunday.setVisibility(View.VISIBLE);

                        final EditText etSundayFrom = findViewById(R.id.etSundayFrom);
                        final EditText etSundayTo = findViewById(R.id.etSundayTo);

                        etSundayFrom.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Calendar mcurrentTime = Calendar.getInstance();
                                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                                int minute = mcurrentTime.get(Calendar.MINUTE);

                                TimePickerDialog mTimePicker;
                                mTimePicker = new TimePickerDialog(Main2Activity.this, new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                                        etSundayFrom.setText( selectedHour + ":" + selectedMinute);
                                    }
                                }, hour, minute, true);
                                mTimePicker.setTitle("Select Time");
                                mTimePicker.show();
                            }
                        });

                        etSundayTo.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Calendar mcurrentTime = Calendar.getInstance();
                                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                                int minute = mcurrentTime.get(Calendar.MINUTE);

                                TimePickerDialog mTimePicker;
                                mTimePicker = new TimePickerDialog(Main2Activity.this, new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                                        etSundayTo.setText( selectedHour + ":" + selectedMinute);
                                    }
                                }, hour, minute, true);
                                mTimePicker.setTitle("Select Time");
                                mTimePicker.show();
                            }
                        });

                        break;
                    }else {
                        text = "Sunday unchecked";
                        break;
                    }
                case R.id.checkBoxMon:
                    text = "Monday";
                    break;
                case R.id.checkBoxTue:
                    text = "Tuesday";
                    break;
                case R.id.checkBoxWed:
                    text = "Tuesday";
                    break;
                case R.id.checkBoxThu:
                    text = "Tuesday";
                    break;
                case R.id.checkBoxFri:
                    text = "Tuesday";
                    break;
                case R.id.checkBoxSat:
                    text = "Tuesday";
                    break;

            }
            Toast.makeText(Main2Activity.this, text, Toast.LENGTH_LONG).show();

        }
    };

}
