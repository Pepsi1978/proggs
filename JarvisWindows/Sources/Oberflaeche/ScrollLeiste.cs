using System.ComponentModel;
using System.Windows;
using System.Windows.Markup;
using System.Windows.Media;

namespace JarvisWindows;

/// <summary>Schmale, runde Bildlaufleisten für die ganze App, ohne helle Spur und ohne Pfeile. Die Farben hängen an DesignStand und wechseln mit Hell und Dunkel.</summary>
public static class ScrollLeiste
{
    private const string Vorlage = """
    <ResourceDictionary xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
      <Style x:Key="Griff" TargetType="Thumb">
        <Setter Property="OverridesDefaultStyle" Value="True"/>
        <Setter Property="IsTabStop" Value="False"/>
        <Setter Property="Template">
          <Setter.Value>
            <ControlTemplate TargetType="Thumb">
              <Border x:Name="Flaeche" CornerRadius="4" Margin="2" Background="{DynamicResource ScrollDaumen}"/>
              <ControlTemplate.Triggers>
                <Trigger Property="IsMouseOver" Value="True"><Setter TargetName="Flaeche" Property="Background" Value="{DynamicResource ScrollDaumenSchwebt}"/></Trigger>
                <Trigger Property="IsDragging" Value="True"><Setter TargetName="Flaeche" Property="Background" Value="{DynamicResource ScrollDaumenAktiv}"/></Trigger>
              </ControlTemplate.Triggers>
            </ControlTemplate>
          </Setter.Value>
        </Setter>
      </Style>
      <Style x:Key="Seite" TargetType="RepeatButton">
        <Setter Property="OverridesDefaultStyle" Value="True"/>
        <Setter Property="IsTabStop" Value="False"/>
        <Setter Property="Focusable" Value="False"/>
        <Setter Property="Template">
          <Setter.Value><ControlTemplate TargetType="RepeatButton"><Border Background="Transparent"/></ControlTemplate></Setter.Value>
        </Setter>
      </Style>
      <Style TargetType="ScrollBar">
        <Setter Property="OverridesDefaultStyle" Value="True"/>
        <Setter Property="Background" Value="Transparent"/>
        <Setter Property="Width" Value="10"/>
        <Setter Property="MinWidth" Value="10"/>
        <Setter Property="Template">
          <Setter.Value>
            <ControlTemplate TargetType="ScrollBar">
              <Border Background="Transparent">
                <Track x:Name="PART_Track" IsDirectionReversed="True">
                  <Track.DecreaseRepeatButton><RepeatButton Style="{StaticResource Seite}" Command="ScrollBar.PageUpCommand"/></Track.DecreaseRepeatButton>
                  <Track.Thumb><Thumb Style="{StaticResource Griff}"/></Track.Thumb>
                  <Track.IncreaseRepeatButton><RepeatButton Style="{StaticResource Seite}" Command="ScrollBar.PageDownCommand"/></Track.IncreaseRepeatButton>
                </Track>
              </Border>
            </ControlTemplate>
          </Setter.Value>
        </Setter>
        <Style.Triggers>
          <Trigger Property="Orientation" Value="Horizontal">
            <Setter Property="Width" Value="Auto"/>
            <Setter Property="MinWidth" Value="0"/>
            <Setter Property="Height" Value="10"/>
            <Setter Property="MinHeight" Value="10"/>
            <Setter Property="Template">
              <Setter.Value>
                <ControlTemplate TargetType="ScrollBar">
                  <Border Background="Transparent">
                    <Track x:Name="PART_Track" IsDirectionReversed="False">
                      <Track.DecreaseRepeatButton><RepeatButton Style="{StaticResource Seite}" Command="ScrollBar.PageLeftCommand"/></Track.DecreaseRepeatButton>
                      <Track.Thumb><Thumb Style="{StaticResource Griff}"/></Track.Thumb>
                      <Track.IncreaseRepeatButton><RepeatButton Style="{StaticResource Seite}" Command="ScrollBar.PageRightCommand"/></Track.IncreaseRepeatButton>
                    </Track>
                  </Border>
                </ControlTemplate>
              </Setter.Value>
            </Setter>
          </Trigger>
        </Style.Triggers>
      </Style>
    </ResourceDictionary>
    """;

    public static void Einrichten(Application app)
    {
        app.Resources.MergedDictionaries.Add((ResourceDictionary)XamlReader.Parse(Vorlage));
        Farben(app);
        DesignStand.Shared.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName is nameof(DesignStand.Dunkel) or null) Farben(app);
        };
    }

    private static void Farben(Application app)
    {
        app.Resources["ScrollDaumen"] = Farbe.Glas(0.22);
        app.Resources["ScrollDaumenSchwebt"] = Farbe.Glas(0.38);
        app.Resources["ScrollDaumenAktiv"] = Farbe.Haupt;
    }
}
