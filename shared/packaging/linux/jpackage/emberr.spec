Summary: APPLICATION_SUMMARY
Name: APPLICATION_PACKAGE
Version: APPLICATION_VERSION
Release: APPLICATION_RELEASE
License: APPLICATION_LICENSE_TYPE
Vendor: APPLICATION_VENDOR

%if "xAPPLICATION_URL" != "x"
URL: APPLICATION_URL
%endif

%if "xAPPLICATION_PREFIX" != "x"
Prefix: APPLICATION_PREFIX
%endif

Provides: APPLICATION_PACKAGE

%if "xAPPLICATION_GROUP" != "x"
Group: APPLICATION_GROUP
%endif

Autoprov: 0
Autoreq: 0
%if "xPACKAGE_DEFAULT_DEPENDENCIES" != "x" || "xPACKAGE_CUSTOM_DEPENDENCIES" != "x"
Requires: PACKAGE_DEFAULT_DEPENDENCIES PACKAGE_CUSTOM_DEPENDENCIES
%endif

#comment line below to enable effective jar compression
#it could easily get your package size from 40 to 15Mb but
#build time will substantially increase and it may require unpack200/system java to install
%define __jar_repack %{nil}

# on RHEL we got unwanted improved debugging enhancements
%define _build_id_links none

%define package_filelist %{_builddir}/%{name}.files
%define app_filelist %{_builddir}/%{name}.app.files
%define filesystem_filelist %{_builddir}/%{name}.filesystem.files

%define default_filesystem / /opt /usr /usr/bin /usr/lib /usr/local /usr/local/bin /usr/local/lib

%define desktop_entry_dir /usr/share/applications
%define mime_package_dir /usr/share/mime/packages
%define icon_theme_dir /usr/share/icons/hicolor

%define refresh_desktop_caches                                            \
if command -v update-desktop-database > /dev/null 2>&1 ; then              \
  update-desktop-database %{desktop_entry_dir} > /dev/null 2>&1 || true    \
fi                                                                        \
if command -v update-mime-database > /dev/null 2>&1 ; then                 \
  update-mime-database /usr/share/mime > /dev/null 2>&1 || true            \
fi                                                                        \
if command -v gtk-update-icon-cache > /dev/null 2>&1 ; then               \
  gtk-update-icon-cache --quiet %{icon_theme_dir} > /dev/null 2>&1 || true \
fi

%description
APPLICATION_DESCRIPTION

%global __os_install_post %{nil}

%prep

%build

%install
rm -rf %{buildroot}
install -d -m 755 %{buildroot}APPLICATION_DIRECTORY
cp -r %{_sourcedir}APPLICATION_DIRECTORY/* %{buildroot}APPLICATION_DIRECTORY
if [ "$(echo %{_sourcedir}/lib/systemd/system/*.service)" != '%{_sourcedir}/lib/systemd/system/*.service' ]; then
  install -d -m 755 %{buildroot}/lib/systemd/system
  cp %{_sourcedir}/lib/systemd/system/*.service %{buildroot}/lib/systemd/system
fi
desktop_integration_dir="%{buildroot}APPLICATION_DIRECTORY/lib"

for desktop_entry in "$desktop_integration_dir"/*.desktop ; do
  [ -f "$desktop_entry" ] || continue
  install -d -m 755 "%{buildroot}%{desktop_entry_dir}"
  install -m 644 "$desktop_entry" "%{buildroot}%{desktop_entry_dir}"
done

for mime_info in "$desktop_integration_dir"/*-MimeInfo.xml ; do
  [ -f "$mime_info" ] || continue
  install -d -m 755 "%{buildroot}%{mime_package_dir}"
  install -m 644 "$mime_info" "%{buildroot}%{mime_package_dir}"

  for mime_type in `sed -n 's|.*<mime-type type="\([^"]*\)".*|\1|p' "$mime_info"` ; do
    mime_icon_name=`echo "$mime_type" | tr '/' '-'`
    for mime_icon in "$desktop_integration_dir/$mime_icon_name".* ; do
      [ -f "$mime_icon" ] || continue
      icon_pixels=`file -b "$mime_icon" 2>/dev/null | sed -n 's/.*, \([0-9]*\) x [0-9]*,.*/\1/p'`
      case "$icon_pixels" in '' | *[!0-9]*) icon_pixels=48 ;; esac
      if [ "$icon_pixels" -le 16 ] ; then theme_size=16
      elif [ "$icon_pixels" -le 22 ] ; then theme_size=22
      elif [ "$icon_pixels" -le 32 ] ; then theme_size=32
      elif [ "$icon_pixels" -le 48 ] ; then theme_size=48
      elif [ "$icon_pixels" -le 64 ] ; then theme_size=64
      else theme_size=128
      fi
      install -d -m 755 "%{buildroot}%{icon_theme_dir}/${theme_size}x${theme_size}/mimetypes"
      install -m 644 "$mime_icon" "%{buildroot}%{icon_theme_dir}/${theme_size}x${theme_size}/mimetypes"
    done
  done
done
%if "xAPPLICATION_LICENSE_FILE" != "x"
  %define license_install_file %{_defaultlicensedir}/%{name}-%{version}/%{basename:APPLICATION_LICENSE_FILE}
  install -d -m 755 "%{buildroot}%{dirname:%{license_install_file}}"
  install -m 644 "APPLICATION_LICENSE_FILE" "%{buildroot}%{license_install_file}"
%endif
(cd %{buildroot} && find . -path ./lib/systemd -prune -o -type d -print) | sed -e 's/^\.//' -e '/^$/d' | LC_ALL=C sort > %{app_filelist}
{ rpm -ql filesystem || echo %{default_filesystem}; } | LC_ALL=C sort > %{filesystem_filelist}
LC_ALL=C comm -23 %{app_filelist} %{filesystem_filelist} > %{package_filelist}
sed -i -e 's/.*/%dir "&"/' %{package_filelist}
(cd %{buildroot} && find . -not -type d) | sed -e 's/^\.//' -e 's/.*/"&"/' >> %{package_filelist}
%if "xAPPLICATION_LICENSE_FILE" != "x"
  sed -i -e 's|"%{license_install_file}"||' -e '/^$/d' %{package_filelist}
%endif

%files -f %{package_filelist}
%if "xAPPLICATION_LICENSE_FILE" != "x"
  %license "%{license_install_file}"
%endif

%post
package_type=rpm
LAUNCHER_AS_SERVICE_SCRIPTS
LAUNCHER_AS_SERVICE_COMMANDS_INSTALL
%{refresh_desktop_caches}

%pre
package_type=rpm
LAUNCHER_AS_SERVICE_SCRIPTS
if [ "$1" = 2 ]; then
  true; LAUNCHER_AS_SERVICE_COMMANDS_UNINSTALL
fi

%preun
package_type=rpm
LAUNCHER_AS_SERVICE_SCRIPTS
LAUNCHER_AS_SERVICE_COMMANDS_UNINSTALL

%postun
%{refresh_desktop_caches}

%clean
