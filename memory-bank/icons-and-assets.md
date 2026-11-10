Use Android vector drawable resources for navigation and category icons, referencing them in layouts or menus as needed:

```xml
<ImageView
  android:id="@+id/nav_dashboard_icon"
  android:contentDescription="@string/title_dashboard"
  android:src="@drawable/ic_nav_home"/>
```

Existing `fa_*` icon keys in stored category data are legacy identifiers mapped by application icon handling; they do not require Font Awesome font assets. Add vector drawables under `res/drawable/` and retain attribution for newly sourced artwork in `THIRD_PARTY_NOTICES.md`.
